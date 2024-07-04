package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import galois.llm.query.utils.QueryUtils;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaListFromAttributes;
import static galois.llm.query.utils.QueryUtils.getCleanAttributes;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;
import java.util.ArrayList;
import java.util.Map;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

@Slf4j
@Getter
public class TogetheraiLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;

    public TogetheraiLlama3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.sql = sql;
    }

    public TogetheraiLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql cannot be null or blank!");
        }
        this.sql = sql;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        List<Attribute> attributesExecution = getCleanAttributes(table);
        if (this.attributes != null && !this.attributes.isEmpty()) {
            attributesExecution = new ArrayList<>();
            for (AttributeRef attribute : this.attributes) {
                attributesExecution.add(table.getAttribute(attribute.getName()));
            }
        }
        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributesExecution);

        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? generateFirstPrompt(table, attributesExecution, null, jsonSchema)
                    : generateIterativePrompt(table, attributesExecution, jsonSchema);
            log.debug("Prompt is: {}", userMessage);
            try {
                String response = super.getResponse(chain, userMessage);
                log.debug("Response is: {}", response);
                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                log.debug("Parsed response is: {}", parsedResponse);
                if (parsedResponse.isEmpty()) {
                    break; // no more iterations
                }
                for (Map<String, Object> map : parsedResponse) {
                    Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                    // TODO: Handle possible duplicates
                    tuples.add(tuple);
                }
            } catch (Exception e) {
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate());
                    log.debug("Response is: {}", response);
                    List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                    log.debug("Parsed response is: {}", parsedResponse);
                    for (Map<String, Object> map : parsedResponse) {
                        Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                        // TODO: Handle possible duplicates
                        tuples.add(tuple);
                    }
                } catch (Exception internal) {
                    // do nothing
                }
            }
        }
        return tuples;
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_8B);
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_70B);
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return firstPrompt.generateUsingSQL(sql, jsonSchema);
    }

    public static TogetheraiLlama3SQLQueryExecutorBuilder builder() {
        return new TogetheraiLlama3SQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3SQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private String sql;

        public TogetheraiLlama3SQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3SQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql
            );
        }
    }
}
