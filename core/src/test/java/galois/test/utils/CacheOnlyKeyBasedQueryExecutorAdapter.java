package galois.test.utils;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.LLMCache;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.mapToTuple;

@Slf4j
@RequiredArgsConstructor
public class CacheOnlyKeyBasedQueryExecutorAdapter extends AbstractKeyBasedQueryExecutor implements ICacheOnlyQueryExecutorAdapter {
    @Getter
    private final IQueryExecutor queryExecutor;

    // The attributes of a key are requested one by one or all together, depending on the wrapped executor
    private final EAttributesStrategy attributesStrategy;

    private boolean cacheMiss = false;

    public CacheOnlyKeyBasedQueryExecutorAdapter(IQueryExecutor queryExecutor) {
        this(queryExecutor, EAttributesStrategy.of(queryExecutor));
    }

    @Override
    protected String getResponse(Chain<String, String> chain, String userMessage, boolean ignoreTokens, int iteration, String firstPrompt) {
        // The executor keeps requesting keys and attributes after an aborted request, but they depend on a response we do not have
        if (cacheMiss) throw new UnsupportedOperationException("Cannot continue without the LLM!");

        LLMCache llmCache = LLMCache.getInstance();
        if (llmCache.containsQuery(userMessage, iteration, queryExecutor, firstPrompt)) {
            CacheEntry entry = llmCache.getResponse(userMessage, iteration, queryExecutor, firstPrompt);
            CacheHitCounter.getInstance().addHit();
            return entry.response();
        }

        log.debug("Cache miss at iteration {} for the prompt: {}", iteration, userMessage);
        CacheHitCounter.getInstance().addMiss();
        cacheMiss = true;
        throw new UnsupportedOperationException("Cannot continue without the LLM!");
    }

    /**
     * Requests the attributes as the wrapped executor does, but through the cache only getResponse of this adapter:
     * delegating to the wrapped executor would make its own requests, and hence query the LLM.
     */
    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, Chain<String, String> chain) {
        if (attributesStrategy == EAttributesStrategy.ALL_ATTRIBUTES) {
            Map<String, Object> attributesMap = getAttributesValues(table, attributes, key, chain);
            return mapToTuple(tuple, attributesMap, tableAlias, attributes);
        }

        Map<String, Object> attributesMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            Map<String, Object> map = getAttributesValues(table, List.of(attribute), key, chain);
            if (map != null) attributesMap.putAll(map);
        }
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        return null;
    }

    @Override
    protected ChatLanguageModel getChatLanguageModel() {
        throw new UnsupportedOperationException("Cannot query the LLM in cache only mode!");
    }

    @Override
    public EPrompts getFirstPrompt() {
        return queryExecutor.getFirstPrompt();
    }

    @Override
    public EPrompts getIterativePrompt() {
        return queryExecutor.getIterativePrompt();
    }

    @Override
    public EPrompts getAttributesPrompt() {
        return queryExecutor.getAttributesPrompt();
    }

    @Override
    public int getMaxIterations() {
        return queryExecutor.getMaxIterations();
    }

    @Override
    public Expression getExpression() {
        return queryExecutor.getExpression();
    }

    @Override
    public boolean ignoreTree() {
        return queryExecutor.ignoreTree();
    }

    @Override
    public boolean ensureKeyInAttributes() {
        return queryExecutor.ensureKeyInAttributes();
    }

    @Override
    public ContentRetriever getContentRetriever() {
        return queryExecutor.getContentRetriever();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return new CacheOnlyKeyBasedQueryExecutorAdapterBuilder(queryExecutor, attributesStrategy);
    }

    public enum EAttributesStrategy {
        // The key scan executors request every attribute of a key with a single prompt, the key ones a prompt each
        ALL_ATTRIBUTES,
        ATTRIBUTE_BY_ATTRIBUTE;

        public static EAttributesStrategy of(IQueryExecutor queryExecutor) {
            String simpleName = queryExecutor.getClass().getSimpleName();
            if (simpleName.contains("KeyScan")) return ALL_ATTRIBUTES;
            if (simpleName.contains("Key")) return ATTRIBUTE_BY_ATTRIBUTE;
            throw new UnsupportedOperationException("Cannot find the attributes strategy of " + simpleName);
        }
    }

    @RequiredArgsConstructor
    public static class CacheOnlyKeyBasedQueryExecutorAdapterBuilder extends AbstractQueryExecutorBuilder {
        private final IQueryExecutor queryExecutor;
        private final EAttributesStrategy attributesStrategy;

        @Override
        public IQueryExecutor build() {
            // The optimizers rebuild the executor, e.g. to push a condition down: the rebuilt one must stay cache only
            IQueryExecutor optimized = queryExecutor.getBuilder()
                    .firstPrompt(getFirstPrompt())
                    .iterativePrompt(getIterativePrompt())
                    .attributesPrompt(getAttributesPrompt())
                    .maxIterations(getMaxIterations())
                    .expression(getExpression())
                    .contentRetriever(getContentRetriever())
                    .build();
            return new CacheOnlyKeyBasedQueryExecutorAdapter(optimized, attributesStrategy);
        }
    }
}
