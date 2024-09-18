package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import galois.llm.TokensEstimator;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.*;
import java.util.stream.Collectors;

import static galois.llm.query.utils.QueryUtils.*;

import galois.prompt.EPrompts;
import galois.utils.GaloisDebug;
import galois.utils.Mapper;

import static galois.utils.Mapper.toCleanJsonList;

@Slf4j
public abstract class AbstractKeyBasedQueryExecutor implements IQueryExecutor {

    private List<AttributeRef> attributes = null;

    abstract protected ConversationalChain getConversationalChain();

    abstract protected ChatLanguageModel getChatLanguageModel();

    protected abstract Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain);

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        Key primaryKey = database.getPrimaryKey(table.getName());
        List<Map<String, Object>> keyValues = getKeyValues(table, primaryKey, chain);
        GaloisDebug.log("Parsed keys are:");
        GaloisDebug.log(keyValues);

        List<Tuple> tuples = keyValues.stream().map(k -> generateTupleFromKey(table, tableAlias, k, primaryKey, chain)).toList();
        GaloisDebug.log("LLMScan results:");
        GaloisDebug.log(tuples);
        return tuples;
    }

    private List<Map<String, Object>> getKeyValues(ITable table, Key primaryKey, ConversationalChain chain) {
//        List<Map<String, Object>> keys = new ArrayList<>();
        Set<Map<String, Object>> keys = new HashSet<>();
        String schema = primaryKey.isCompositeKey() ?
                generateJsonSchemaForCompositePrimaryKey(table, primaryKey) :
                generateJsonSchemaForPrimaryKey(table);

        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? getFirstPrompt().generate(table, primaryKey, getExpression(), schema)
                    : getIterativePrompt().generate(table, primaryKey, getExpression(), schema);
            log.debug("Key prompt is: {}", userMessage);
            String response = "";
            Boolean callGetResponse = null;
            try {
                log.trace("Asking for response...");
                callGetResponse = false;
                response = getResponse(chain, userMessage, false);
                callGetResponse = true;
                log.debug("Response is: {}", response);
                if (response == null || response.trim().isBlank()) break; // avoid other requests
                List<Map<String, Object>> currentKeys = parseKeyResponse(response, table, primaryKey);
                log.debug("Parsed keys are: {}", currentKeys);
                if (currentKeys.isEmpty()) break; // avoid other requests
                String cleanedResponse = toCleanJsonList(response);
                log.debug("Cleaned response is : {}", cleanedResponse);
                currentKeys = parseKeyResponse(cleanedResponse, table, primaryKey);
                if (!currentKeys.isEmpty()) {
                    keys.addAll(currentKeys);
                } else {
                    break;
                }
            } catch (Exception e) {
                log.trace("Exception - Key prompt is: {}", userMessage);
                log.trace("Exception - Response is: {}", response);
                log.trace("Exception: {}", e);
                if (callGetResponse != null && !callGetResponse && e instanceof NullPointerException) {
                    log.trace("Stop making request!!");
                    break; // it is a failure of the model since in th request in get Response we already attempted!
                }
            }
        }
//        boolean debug = true;
//        int debugSize = 10;
//        if (debug) {
//            return keys.subList(0, debugSize);
//        }
//        return keys;
        return new ArrayList<>(keys);
    }

    private List<Map<String, Object>> parseKeyResponse(String response, ITable table, Key primaryKey) {
        List<Map<String, Object>> currentKeys = new ArrayList<>();
        if (primaryKey.isCompositeKey()) {
            currentKeys = getFirstPrompt().getEntitiesParser().parse(response, table);
        } else {
            List<String> keys = getFirstPrompt().getKeyParser().parse(response);
            for (String key : keys) {
                Map<String, Object> keyMap = Map.of(primaryKey.getAttributes().getFirst().getName(), key);
                currentKeys.add(keyMap);
            }
        }
        return currentKeys;
    }

    private Tuple generateTupleFromKey(ITable table, TableAlias tableAlias, Map<String, Object> keyValue, Key primaryKey, ConversationalChain chain) {
        List<String> primaryKeyAttributes = primaryKey.getAttributes().stream().map(AttributeRef::getName).toList();
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(tuple, tableAlias, keyValue, primaryKeyAttributes);
        Set<Attribute> attributesQuery = null;
        if (this.attributes != null && !this.attributes.isEmpty()) {
            attributesQuery = new HashSet<>();
            for (AttributeRef aRef : this.attributes) {
                // Guardrail for existing attributes
                Attribute attribute = table.getAttributes().stream()
                        .filter(a -> a.getName().equalsIgnoreCase(aRef.getName()))
                        .findFirst()
                        .orElse(null);
                if (attribute != null && !attribute.getName().equals("oid") && !primaryKeyAttributes.contains(attribute.getName())) {
                    attributesQuery.add(attribute);
                }
            }
        } else {
            attributesQuery = table.getAttributes().stream()
                    .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                    .collect(Collectors.toSet());
        }

        if (attributesQuery.isEmpty()) {
            return tuple;
        }

        String keyAsString = getKeyAsString(keyValue, primaryKeyAttributes);
        return addValueFromAttributes(table, tableAlias, new ArrayList<>(attributesQuery), tuple, keyAsString, chain);
    }

    private void addCellForPrimaryKey(Tuple tuple, TableAlias tableAlias, Map<String, Object> key, List<String> primaryKeyAttributes) {
        for (String attribute : primaryKeyAttributes) {
            Cell keyCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attribute),
                    new ConstantValue(key.get(attribute))
            );
            tuple.addCell(keyCell);
        }
    }

    private String getKeyAsString(Map<String, Object> key, List<String> primaryKeyAttributes) {
        StringBuilder builder = new StringBuilder().append(key.get(primaryKeyAttributes.getFirst()));
        if (primaryKeyAttributes.size() == 1) {
            return builder.toString();
        }

        builder.append(" ( ");
        for (int i = 1; i < primaryKeyAttributes.size(); i++) {
            builder.append(key.get(primaryKeyAttributes.get(i)));
            if (i + 1 < primaryKeyAttributes.size()) {
                builder.append(", ");
            }
        }
        builder.append(" )");
        return builder.toString();
    }

    protected Map<String, Object> getAttributesValues(ITable table, List<Attribute> attributesPrompt, String key, ConversationalChain chain) {
        String jsonSchema = generateJsonSchemaFromAttributes(table, attributesPrompt);
        String prompt = getAttributesPrompt().generate(table, key, attributesPrompt, jsonSchema);
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
                response = getResponse(newChain, prompt, false);
                log.debug("Attribute response is: {}", response);
                if (!Mapper.isJSON(response)) {
                    response = getResponse(newChain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), true);
                    log.debug("Attribute response is after appropriate JSON format: {}", response);
                    return getAttributesPrompt().getAttributesParser().parse(response, attributesPrompt);
                } else {
                    log.debug("Attribute response is: {}", response);
                    return getAttributesPrompt().getAttributesParser().parse(response, attributesPrompt);
                }
            }
            return getAttributesPrompt().getAttributesParser().parse(response, attributesPrompt);
        } catch (Exception e) {
            log.debug("Prompt: \n" + prompt);
            log.debug("Response: \n " + response);
            log.debug("Exception: \n" + e);
            try {
                if (newChain == null) {
                    newChain = getConversationalChain();
                    response = getResponse(newChain, prompt, false);
                }
                response = getResponse(newChain, EPrompts.ERROR_JSON_NUMBER_FORMAT.getTemplate(), false);
                log.debug("Exception - Attribute response is after appropriate JSON format: {}", response);
                return getAttributesPrompt().getAttributesParser().parse(response, attributesPrompt);
            } catch (Exception internal) {
                return new HashMap<>();
            }
        }
    }

    private String getResponse(ConversationalChain chain, String userMessage, boolean ignoreTokens) {
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
}
