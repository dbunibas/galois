package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.TokensEstimator;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.*;
import galois.prompt.EPrompts;
import speedy.model.expressions.Expression;

@Slf4j
public abstract class AbstractEntityQueryExecutor implements IQueryExecutor {

    protected List<AttributeRef> attributes = null;

    abstract protected ConversationalChain getConversationalChain();

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());
        log.trace("Table: {}", table);
        List<Attribute> attributesExecution = getCleanAttributes(table);
        log.trace("attributes: {}", attributes);
        if (this.attributes != null && !this.attributes.isEmpty()) {
            attributesExecution = new ArrayList<>();
            for (AttributeRef attribute : this.attributes) {
                attributesExecution.add(table.getAttribute(attribute.getName()));
            }
        }
        log.trace("attributesExecution: {}", attributesExecution);
        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributesExecution);
        Expression expression = getExpression();
        log.debug("Expression: {}", expression);
        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? generateFirstPrompt(table, attributesExecution, getExpression(), jsonSchema)
                    : generateIterativePrompt(table, attributesExecution, jsonSchema);
            log.debug("Prompt is: {}", userMessage);
            try {
                String response = getResponse(chain, userMessage, false);
                log.debug("Response is: {}", response);
                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                log.debug("Parsed response is: {}", parsedResponse);
                if (parsedResponse.isEmpty()) {
                    break; // no more iterations
                }
                for (Map<String, Object> map : parsedResponse) {
                    Tuple tuple = mapToTuple(map, tableAlias, attributesExecution);
                    // TODO: Handle possible duplicates
                    if (tuple != null) tuples.add(tuple);
                }
            } catch (Exception e) {
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), true);
                    log.debug("Response is: {}", response);
                    List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                    log.debug("Parsed response is: {}", parsedResponse);
                    for (Map<String, Object> map : parsedResponse) {
                        Tuple tuple = mapToTuple(map, tableAlias, attributesExecution);
                        // TODO: Handle possible duplicates
                        if (tuple != null) tuples.add(tuple);
                    }
                } catch (Exception internal) {
                    // do nothing
                }
            }
        }
        return tuples;
    }

    protected String getResponse(ConversationalChain chain, String userMessage, boolean ignoreTokens) {
        TokensEstimator estimator = new TokensEstimator();
        // TODO [Stats:] TokenCountEstimator estimator get from model
        LLMQueryStatManager queryStatManager = LLMQueryStatManager.getInstance();
        double inputTokens = estimator.getTokens(userMessage);
        if (!ignoreTokens) queryStatManager.updateLLMTokensInput(inputTokens);
        long start = System.currentTimeMillis();
        String response = chain.execute(userMessage);
        if (!ignoreTokens) queryStatManager.updateTimeMs(System.currentTimeMillis() - start);
        double outputTokens = estimator.getTokens(response);
        if (!ignoreTokens) queryStatManager.updateLLMTokensOutput(outputTokens);
        if (!ignoreTokens) queryStatManager.updateLLMRequest(1);
        return response;
    }

    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return getFirstPrompt().generate(table, attributes, expression, jsonSchema);
    }

    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getIterativePrompt().generate(table, attributes, jsonSchema);
    }
}
