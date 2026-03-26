package bsf.llm.query.utils.cache;

import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.utils.cache.db.DBCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bsf.Constants.CACHE_ENABLED;

@Slf4j
public class LLMCache {
    @Getter
    private static final LLMCache instance = new LLMCache();

    private final ILLMCache implementation = new DBCache();

    private LLMCache() {
    }


    public boolean containsQuery(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!CACHE_ENABLED) return false;
        return implementation.containsQuery(prompt, iteration, queryExecutor, firstPrompt);
    }

    public CacheEntry getResponse(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!CACHE_ENABLED) throw new UnsupportedOperationException("Cannot use cache when it is disabled!");
        return implementation.getResponse(prompt, iteration, queryExecutor, firstPrompt);
    }

    public void updateCache(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt, String response, double inputTokens, double outputTokens, long timeMillis, int baseLLMRequestsIncrement) {
        if (!CACHE_ENABLED) return;
        implementation.updateCache(prompt, iteration, queryExecutor, firstPrompt, response, inputTokens, outputTokens, timeMillis, baseLLMRequestsIncrement);
    }
}
