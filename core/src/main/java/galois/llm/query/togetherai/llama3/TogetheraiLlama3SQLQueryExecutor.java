package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.*;
import galois.llm.query.utils.QueryUtils;
import galois.prompt.EPrompts;
import galois.utils.Configuration;
import galois.utils.ExternalKnowledgeGenerator;
import galois.utils.GaloisDebug;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;
import speedy.model.expressions.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildTogetherAIConversationalRetrivalChain;
import static galois.llm.query.utils.QueryUtils.*;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
@Setter
public class TogetheraiLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor implements ISQLExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private String sql;
    private final ContentRetriever contentRetriever;

    public TogetheraiLlama3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.sql = sql;
        this.contentRetriever = null;
    }

    public TogetheraiLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql, ContentRetriever contentRetriever) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        this.contentRetriever = contentRetriever;
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql cannot be null or blank!");
        }
        this.sql = sql;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias, Double llmProbThreshold) {
        Chain<String, String> chain = getConversationalChain();

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
                String response = super.getResponse(chain, userMessage, i, false, generateFirstPrompt(table, attributesExecution, null, jsonSchema));
                log.debug("Response is: {}", response);
                if (response == null || response.trim().isBlank()) {
                    log.warn("Error during LLM request.");
                    break;
                }
                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                log.debug("Parsed response is: {}", parsedResponse);
                if (parsedResponse == null || parsedResponse.isEmpty()) {
                    break; // no more iterations
                }
                int initialTuples = tuples.size();
                for (Map<String, Object> map : parsedResponse) {
                    Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                    if (!isAlreadyContained(tuple, tuples)) {
                        tuples.add(tuple);
                    }
                }
                if (tuples.size() == initialTuples) {
                    log.info("Iteration {} did not add any new tuples. Avoid proceeding with further iterations", i);
                    return tuples;
                }
            } catch (Exception e) {
                log.debug("Exception", e);
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), i, true, generateFirstPrompt(table, attributesExecution, null, jsonSchema));
                    log.debug("Response is: {}", response);
                    List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                    log.debug("Parsed response is: {}", parsedResponse);
                    for (Map<String, Object> map : parsedResponse) {
                        Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                        if (!isAlreadyContained(tuple, tuples)) {
                            tuples.add(tuple);
                        }
                    }
                } catch (Exception internal) {
                    // do nothing
                }
            }
        }
        GaloisDebug.log("LLMScan results:");
        GaloisDebug.log(tuples);
        return tuples;
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (contentRetriever == null) {
            return buildTogetherAIConversationalChain(Configuration.getInstance().getTogetheraiApiKey(), Configuration.getInstance().getTogetheraiModel());
        } else {
            return buildTogetherAIConversationalRetrivalChain(Configuration.getInstance().getTogetheraiApiKey(), Configuration.getInstance().getTogetheraiModel(), contentRetriever);
        }
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        if (ExternalKnowledgeGenerator.getInstance().isGenerate()) {
            ExternalKnowledgeGenerator.getInstance().setTable(table);
        }

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
                    sql,
                    getContentRetriever()
            );
        }
    }
}
