package bsf.llm.query.utils.cache;

import bsf.llm.query.IQueryExecutor;

public interface ILLMCache {
    boolean containsQuery(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt);

    CacheEntry getResponse(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt);

    void updateCache(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt, String response, double inputTokens, double outputTokens, long timeMillis, int baseLLMRequestsIncrement);
}
