package galois.llm.query;

import dev.langchain4j.chain.Chain;
import galois.llm.TokensEstimator;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.LLMCache;
import galois.prompt.EPrompts;
import galois.utils.GaloisDebug;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;
import speedy.model.expressions.Expression;

import java.util.*;

import static galois.llm.query.utils.QueryUtils.*;

@Slf4j
public abstract class AbstractEntityQueryExecutor implements IQueryExecutor {

    protected List<AttributeRef> attributes = null;

    abstract protected Chain<String, String> getConversationalChain();

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        Chain<String, String> chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());
        log.trace("Table: {}", table);
        log.trace("attributes: {}", attributes);
        Set<Attribute> attributesExecution = null;
        if (this.attributes != null && !this.attributes.isEmpty()) {
            attributesExecution = new HashSet<>();
            for (AttributeRef aRef : this.attributes) {
                // Guardrail for existing attributes
                Attribute attribute = table.getAttributes().stream()
                        .filter(a -> a.getName().equalsIgnoreCase(aRef.getName()))
                        .findFirst()
                        .orElse(null);
                if (attribute == null) {
                    continue;
                }

                attributesExecution.add(table.getAttribute(attribute.getName()));
            }

            if (ensureKeyInAttributes()) {
                Key primaryKey = database.getPrimaryKey(table.getName());
                for (AttributeRef attribute : primaryKey.getAttributes()) {
                    attributesExecution.add(table.getAttribute(attribute.getName()));
                }
            }
        }

        List<Attribute> attributesExecutionList = attributesExecution == null ?
                getCleanAttributes(table) :
                new ArrayList<>(attributesExecution);
        log.trace("attributesExecutionList: {}", attributesExecution);

        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributesExecutionList);
        Expression expression = getExpression();
        log.debug("Expression: {}", expression);
        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? generateFirstPrompt(table, attributesExecutionList, getExpression(), jsonSchema)
                    : generateIterativePrompt(table, attributesExecutionList, jsonSchema);
            log.debug("Iteration {} - Prompt is: {}", i, userMessage);
            try {
                String response = getResponse(chain, userMessage, i, false, generateFirstPrompt(table, attributesExecutionList, getExpression(), jsonSchema));
                log.debug("Response is: {}", response);
                if (response == null || response.trim().isBlank()) {
                    log.warn("Error during LLM request.");
                    break;
                }
                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                log.debug("Parsed response is: {}", parsedResponse);
                if (parsedResponse.isEmpty()) {
                    GaloisDebug.log("LLMScan results:");
                    GaloisDebug.log(tuples);
                    return tuples;
                }
                int initialTuples = tuples.size();
                for (Map<String, Object> map : parsedResponse) {
                    Tuple tuple = mapToTuple(map, tableAlias, attributesExecutionList);
                    if (tuple != null && !isAlreadyContained(tuple, tuples)) {
                        tuples.add(tuple);
                        log.trace("Adding new tuple {}", tuple);
                    } else {
                        log.trace("Skipping duplicated tuple {}", tuple);
                    }
                }
                log.info("Tuples after {} iterations: {}", i, tuples.size());
                if (tuples.size() == initialTuples) {
                    log.info("Iteration {} did not add any new tuples. Avoid proceeding with further iterations", i);
                    return tuples;
                }
            } catch (Exception e) {
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), i, true, generateFirstPrompt(table, attributesExecutionList, getExpression(), jsonSchema));
                    log.debug("Response is: {}", response);
                    List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                    log.debug("Parsed response is: {}", parsedResponse);
                    for (Map<String, Object> map : parsedResponse) {
                        Tuple tuple = mapToTuple(map, tableAlias, attributesExecutionList);
                        if (tuple != null && !isAlreadyContained(tuple, tuples)) {
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

    protected String getResponse(Chain<String, String> chain, String userMessage, int iteration, boolean ignoreTokens, String firstPrompt) {
        String response = null;
        LLMCache llmCache = LLMCache.getInstance();

        if (llmCache.containsQuery(userMessage, iteration, this, firstPrompt)) {
            log.debug("Cache hit for {} - iteration {}, returning cached value!", userMessage, iteration);
            CacheEntry entry = llmCache.getResponse(userMessage, iteration, this, firstPrompt);
            if (!ignoreTokens) {
                if (entry.baseLLMRequestsIncrement() > 0) {
                    updateBaseStats(entry.inputTokens(), entry.outputTokens(), entry.timeMillis(), entry.baseLLMRequestsIncrement());
                } else {
                    updateStats(entry.inputTokens(), entry.outputTokens(), entry.timeMillis());
                }
            }
            return entry.response();
        }

        try {
            TokensEstimator estimator = new TokensEstimator();
            double inputTokens = estimator.getTokens(userMessage);
            long start = System.currentTimeMillis();
//            log.warn("*** USER MESSAGE: {}", userMessage);
//            //TODO: Improve retrieval for prompts > 0. IDEA: Inject different values from first prompt
//            if(iteration > 0 && (chain instanceof CustomConversionalRetrievalChain customConversionalRetrievalChain)){
//                chain = ConversationalChain.builder().chatLanguageModel(customConversionalRetrievalChain.getChatLanguageModel()).build();
//                ContentRetriever contentRetriever = customConversionalRetrievalChain.getContentRetriever();
////                generateFirstPrompt()
////                contentRetriever.retrieve(Query.from())
//            }
            double baseInputTokens = LLMQueryStatManager.getInstance().getBaseLLMTokensInput();
            double baseOutputTokens = LLMQueryStatManager.getInstance().getBaseLLMTokensOutput();
            long baseTimeMillis = LLMQueryStatManager.getInstance().getBasetimeMs();
            int baseLLMRequests = LLMQueryStatManager.getInstance().getBaseLLMRequest();

            response = chain.execute(userMessage);
            double outputTokens = estimator.getTokens(response);
            long timeMillis = System.currentTimeMillis() - start;
            if (!ignoreTokens) updateStats(inputTokens, outputTokens, timeMillis);

            double baseInputTokensIncrement = LLMQueryStatManager.getInstance().getBaseLLMTokensInput() - baseInputTokens;
            double baseOutputTokensIncrement = LLMQueryStatManager.getInstance().getBaseLLMTokensOutput() - baseOutputTokens;
            long baseTimeMillisIncrement = LLMQueryStatManager.getInstance().getBasetimeMs() - baseTimeMillis;
            int baseLLMRequestsIncrement = LLMQueryStatManager.getInstance().getBaseLLMRequest() - baseLLMRequests;

            inputTokens = baseLLMRequestsIncrement > 0 ? baseInputTokensIncrement : inputTokens;
            outputTokens = baseLLMRequestsIncrement > 0 ? baseOutputTokensIncrement : outputTokens;
            timeMillis = baseLLMRequestsIncrement > 0 ? baseTimeMillisIncrement : timeMillis;
            llmCache.updateCache(userMessage, iteration, this, firstPrompt, response, inputTokens, outputTokens, timeMillis, baseLLMRequestsIncrement);
            return response;
        } catch (Exception e) {
            return response;
        }
    }

    private void updateStats(double inputTokens, double outputTokens, long timeMillis) {
        // TODO [Stats:] TokenCountEstimator estimator get from model
        LLMQueryStatManager queryStatManager = LLMQueryStatManager.getInstance();
        queryStatManager.updateLLMTokensInput(inputTokens);
        queryStatManager.updateLLMTokensOutput(outputTokens);
        queryStatManager.updateTimeMs(timeMillis);
        queryStatManager.updateLLMRequest(1);
    }

    private void updateBaseStats(double inputTokens, double outputTokens, long timeMillis, int baseIncrement) {
        LLMQueryStatManager queryStatManager = LLMQueryStatManager.getInstance();
        queryStatManager.updateBaseLLMTokensInput(inputTokens);
        queryStatManager.updateBaseLLMTokensOutput(outputTokens);
        queryStatManager.updateBaseTimeMs(timeMillis);
        queryStatManager.updateBaseLLMRequest(1);
    }

    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return getFirstPrompt().generate(table, attributes, expression, jsonSchema);
    }

    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getIterativePrompt().generate(table, attributes, jsonSchema);
    }
}
