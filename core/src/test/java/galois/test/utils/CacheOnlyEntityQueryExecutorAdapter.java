package galois.test.utils;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.LLMCache;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.expressions.Expression;

@Slf4j
@RequiredArgsConstructor
public class CacheOnlyEntityQueryExecutorAdapter extends AbstractEntityQueryExecutor implements ICacheOnlyQueryExecutorAdapter {
    @Getter
    private final IQueryExecutor queryExecutor;

    private boolean cacheMiss = false;

    @Override
    protected String getResponse(Chain<String, String> chain, String userMessage, int iteration, boolean ignoreTokens, String firstPrompt) {
        // The executor keeps iterating after an aborted request, but the next prompts depend on a response we do not have
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

    @Override
    protected Chain<String, String> getConversationalChain() {
        return null;
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
        return new CacheOnlyQueryExecutorBuilder(queryExecutor);
    }

    @RequiredArgsConstructor
    public static class CacheOnlyQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private final IQueryExecutor queryExecutor;

        @Override
        public IQueryExecutor build() {
            IQueryExecutor optimized = queryExecutor.getBuilder()
                    .firstPrompt(getFirstPrompt())
                    .iterativePrompt(getIterativePrompt())
                    .attributesPrompt(getAttributesPrompt())
                    .maxIterations(getMaxIterations())
                    .expression(getExpression())
                    .contentRetriever(getContentRetriever())
                    .build();
            return new CacheOnlyEntityQueryExecutorAdapter(optimized);
        }
    }
}
