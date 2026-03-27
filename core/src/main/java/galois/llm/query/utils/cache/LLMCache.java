package galois.llm.query.utils.cache;

import galois.llm.query.IQueryExecutor;
import galois.llm.query.utils.cache.db.DBCache;
import galois.utils.Configuration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMCache {
    @Getter
    private static final LLMCache instance = new LLMCache();

    private final ILLMCache implementation = new DBCache();

    private LLMCache() {
    }


    public boolean containsQuery(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!Configuration.getInstance().isCacheEnabled()) return false;
        return implementation.containsQuery(prompt, iteration, queryExecutor, firstPrompt);
    }

    public CacheEntry getResponse(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!Configuration.getInstance().isCacheEnabled()) throw new UnsupportedOperationException("Cannot use cache when it is disabled!");
        return implementation.getResponse(prompt, iteration, queryExecutor, firstPrompt);
    }

    public void updateCache(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt, String response, double inputTokens, double outputTokens, long timeMillis, int baseLLMRequestsIncrement) {
        if (!Configuration.getInstance().isCacheEnabled()) return;
        implementation.updateCache(prompt, iteration, queryExecutor, firstPrompt, response, inputTokens, outputTokens, timeMillis, baseLLMRequestsIncrement);
    }
}
