package galois.test.utils;

import dev.langchain4j.chain.Chain;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.ISQLExecutor;
import galois.llm.query.openai.OpenAISQLQueryExecutor.OpenAISQLQueryExecutorBuilder;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor.TogetheraiLlama3SQLQueryExecutorBuilder;
import galois.llm.query.utils.QueryUtils;
import galois.prompt.EPrompts;
import galois.utils.ExternalKnowledgeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.generateJsonSchemaListFromAttributes;
import static galois.llm.query.utils.QueryUtils.getCleanAttributes;
import static galois.llm.query.utils.QueryUtils.isAlreadyContained;

/**
 * Replays the requests of a SQL executor, e.g. TogetheraiLlama3SQLQueryExecutor or OpenAISQLQueryExecutor, from the
 * cache only: those executors iterate with their own execute method, which is mirrored here.
 */
@Slf4j
public class CacheOnlySQLQueryExecutorAdapter extends CacheOnlyEntityQueryExecutorAdapter implements ISQLExecutor {

    public CacheOnlySQLQueryExecutorAdapter(IQueryExecutor queryExecutor) {
        super(queryExecutor);
    }

    // Mirrors TogetheraiLlama3SQLQueryExecutor#execute: its prompts, and hence its cache keys, differ from the entity ones
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
        String firstPrompt = generateFirstPrompt(table, attributesExecution, null, jsonSchema);

        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0 ? firstPrompt : generateIterativePrompt(table, attributesExecution, jsonSchema);
            log.debug("Prompt is: {}", userMessage);
            try {
                String response = getResponse(chain, userMessage, i, false, firstPrompt);
                if (response == null || response.trim().isBlank()) break;

                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                if (parsedResponse == null || parsedResponse.isEmpty()) break; // no more iterations

                int initialTuples = tuples.size();
                addTuples(parsedResponse, tableAlias, tuples);
                if (tuples.size() == initialTuples) {
                    log.info("Iteration {} did not add any new tuples. Avoid proceeding with further iterations", i);
                    return tuples;
                }
            } catch (Exception e) {
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), i, true, firstPrompt);
                    addTuples(getFirstPrompt().getEntitiesParser().parse(response, table), tableAlias, tuples);
                } catch (Exception internal) {
                    // do nothing, as the wrapped executor does
                }
            }
        }
        return tuples;
    }

    private void addTuples(List<Map<String, Object>> parsedResponse, TableAlias tableAlias, List<Tuple> tuples) {
        for (Map<String, Object> map : parsedResponse) {
            Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
            if (!isAlreadyContained(tuple, tuples)) tuples.add(tuple);
        }
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        if (ExternalKnowledgeGenerator.getInstance().isGenerate()) {
            ExternalKnowledgeGenerator.getInstance().setTable(table);
        }

        return getFirstPrompt().generateUsingSQL(getSql(), jsonSchema);
    }

    @Override
    public String getSql() {
        return ((ISQLExecutor) getQueryExecutor()).getSql();
    }

    @Override
    public void setSql(String sql) {
        ((ISQLExecutor) getQueryExecutor()).setSql(sql);
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return new CacheOnlySQLQueryExecutorAdapterBuilder(getQueryExecutor());
    }

    @RequiredArgsConstructor
    public static class CacheOnlySQLQueryExecutorAdapterBuilder extends AbstractQueryExecutorBuilder {
        private final IQueryExecutor queryExecutor;

        @Override
        public IQueryExecutor build() {
            // The optimizers rebuild the executor, e.g. to push a condition down: the rebuilt one must stay cache only
            IQueryExecutorBuilder builder = queryExecutor.getBuilder()
                    .firstPrompt(getFirstPrompt())
                    .iterativePrompt(getIterativePrompt())
                    .attributesPrompt(getAttributesPrompt())
                    .maxIterations(getMaxIterations())
                    .expression(getExpression())
                    .contentRetriever(getContentRetriever());
            return new CacheOnlySQLQueryExecutorAdapter(withSql(builder).build());
        }

        // The SQL query is required by the executors, but it is not part of the common builder
        private IQueryExecutorBuilder withSql(IQueryExecutorBuilder builder) {
            String sql = ((ISQLExecutor) queryExecutor).getSql();
            if (builder instanceof TogetheraiLlama3SQLQueryExecutorBuilder togetherai)
                return togetherai.sql(sql);
            if (builder instanceof OpenAISQLQueryExecutorBuilder openai)
                return openai.sql(sql);
            throw new UnsupportedOperationException("Cannot rebuild the executor " + queryExecutor.getClass().getSimpleName());
        }
    }
}
