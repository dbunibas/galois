package galois.test.utils;

import galois.llm.query.IQueryExecutor;

// Adapter that replays a query executor against the cache only, counting the requests in the CacheHitCounter
public interface ICacheOnlyQueryExecutorAdapter {

    IQueryExecutor getQueryExecutor();
}
