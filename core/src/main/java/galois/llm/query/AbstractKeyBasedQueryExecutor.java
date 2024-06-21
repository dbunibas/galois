package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import galois.llm.TokensEstimator;
import galois.llm.models.IModel;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.*;

import static galois.llm.query.utils.QueryUtils.*;
import galois.prompt.EPrompts;
import galois.utils.Mapper;

@Slf4j
public abstract class AbstractKeyBasedQueryExecutor implements IQueryExecutor {

    abstract protected ConversationalChain getConversationalChain();

    abstract protected ChatLanguageModel getChatLanguageModel();

    protected abstract Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain);

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        Key primaryKey = database.getPrimaryKey(table.getName());
        Set<String> keyValues = getKeyValues(table, primaryKey, chain);

        return keyValues.stream().map(k -> generateTupleFromKey(table, tableAlias, k, primaryKey, chain)).toList();
    }

    private Set<String> getKeyValues(ITable table, Key primaryKey, ConversationalChain chain) {
        Set<String> keys = new HashSet<>();
        String schema = generateJsonSchemaForKeys(table);

        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? getFirstPrompt().generate(table, primaryKey, getExpression(), schema)
                    : getIterativePrompt().generate(table, primaryKey, getExpression(), schema);
            log.debug("Key prompt is: {}", userMessage);
            String response = "";
            try {
                response = getResponse(chain, userMessage);
                log.debug("Response is: {}", response);
                List<String> currentKeys = getFirstPrompt().getKeyParser().parse(response);
                log.debug("Parsed keys are: {}", currentKeys);
                keys.addAll(currentKeys);
            } catch (Exception e) {
                log.debug("Key prompt is: \n" + userMessage);
                log.debug("Response is: \n" + response);
                log.debug("Exception: \n" + e);
            }
        }
        return keys;
    }

    private Tuple generateTupleFromKey(ITable table, TableAlias tableAlias, String keyValue, Key primaryKey, ConversationalChain chain) {
        List<String> primaryKeyAttributes = primaryKey.getAttributes().stream().map(AttributeRef::getName).toList();
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(tuple, tableAlias, keyValue, primaryKeyAttributes);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                .toList();

        return addValueFromAttributes(table, tableAlias, attributes, tuple, keyValue, chain);
    }

    private void addCellForPrimaryKey(Tuple tuple, TableAlias tableAlias, String key, List<String> primaryKeyAttributes) {
        // TODO: Handle composite key
        Cell keyCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, primaryKeyAttributes.get(0)),
                new ConstantValue(key)
        );
        tuple.addCell(keyCell);
    }

    protected Map<String, Object> getAttributesValues(ITable table, List<Attribute> attributes, String key, ConversationalChain chain) {
        String jsonSchema = generateJsonSchemaFromAttributes(table, attributes);
        String prompt = getAttributesPrompt().generate(table, key, attributes, jsonSchema);
        log.debug("Attribute prompt is: {}", prompt);
        String response = "";
        ConversationalChain newChain = null;
        try {
            ChatLanguageModel chatLanguageModel = getChatLanguageModel();
            response = chatLanguageModel.generate(prompt);
            //response = getResponse(chain, prompt);
            log.debug("Attribute response is: {}", response);
            if (!Mapper.isJSON(response)) {
                log.debug("Response not in JSON format...execute it again through new chain");
                newChain = getConversationalChain();
                response = getResponse(newChain, prompt);
                log.debug("Attribute response is: {}", response);
                if (!Mapper.isJSON(response)) {
                    response = getResponse(newChain, EPrompts.ERROR_JSON_FORMAT.getTemplate());
                    log.debug("Attribute response is after appropriate JSON format: {}", response);
                    return getAttributesPrompt().getAttributesParser().parse(response, attributes);
                } else {
                    log.debug("Attribute response is: {}", response);
                    return getAttributesPrompt().getAttributesParser().parse(response, attributes);
                }
            }
            return getAttributesPrompt().getAttributesParser().parse(response, attributes);
        } catch (Exception e) {
            log.debug("Prompt: \n" + prompt);
            log.debug("Response: \n " + response);
            log.debug("Exception: \n" + e);
            try {
                if (newChain == null) {
                    newChain = getConversationalChain();
                    response = getResponse(newChain, prompt);
                }
                response = getResponse(newChain, EPrompts.ERROR_JSON_NUMBER_FORMAT.getTemplate());
                log.debug("Exception - Attribute response is after appropriate JSON format: {}", response);
                return getAttributesPrompt().getAttributesParser().parse(response, attributes);
            } catch (Exception internal) {
                return new HashMap<>();
            }
        }
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
}
