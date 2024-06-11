package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.TokensEstimator;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.*;

@Slf4j
public abstract class AbstractEntityQueryExecutor implements IQueryExecutor {

    abstract protected ConversationalChain getConversationalChain();

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        List<Attribute> attributes = getCleanAttributes(table);
        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributes);

        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? generateFirstPrompt(table, attributes, jsonSchema)
                    : generateIterativePrompt(table, attributes, jsonSchema);
            log.debug("Prompt is: {}", userMessage);
            String response = getResponse(chain, userMessage);
            log.debug("Response is: {}", response);

            List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
            log.debug("Parsed response is: {}", parsedResponse);

            for (Map<String, Object> map : parsedResponse) {
                Tuple tuple = mapToTuple(map, tableAlias, attributes);
                // TODO: Handle possible duplicates
                tuples.add(tuple);
            }
        }
        return tuples;
    }

    private String getResponse(ConversationalChain chain, String userMessage) {
        TokensEstimator estimator = new TokensEstimator();
        // TODO [Stats:] TokenCountEstimator estimator get from model
        LLMQueryStatManager queryStatManager = LLMQueryStatManager.getInstance();
        double inputTokens = estimator.getTokens(userMessage);
        queryStatManager.updateLLMTokensInput(inputTokens);
        long start = System.currentTimeMillis();
        String response = chain.execute(userMessage);
        queryStatManager.updateTimeMs(System.currentTimeMillis() - start);
        double outputTokens = estimator.getTokens(response);
        queryStatManager.updateLLMTokensOutput(outputTokens);
        queryStatManager.updateLLMRequest(1);
        return response;
    }

    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getFirstPrompt().generate(table, attributes, jsonSchema);
    }

    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getIterativePrompt().generate(table, attributes, jsonSchema);
    }
}
