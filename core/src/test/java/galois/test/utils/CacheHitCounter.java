package galois.test.utils;

import lombok.Getter;
import lombok.Setter;

// Counts the cache lookups performed while replaying the experiments without ever calling the LLM
@Getter
public class CacheHitCounter {
    private static final CacheHitCounter instance = new CacheHitCounter();

    // When enabled the LLM judge does not query the LLM on a cache miss, see LLMDistance
    @Setter
    private boolean cacheOnly = false;

    // Requests of the query executors
    private long hits = 0;
    private long misses = 0;

    // Requests of the LLM judge, counted apart because they are comparisons of the results, not queries
    private long judgeHits = 0;
    private long judgeMisses = 0;

    private CacheHitCounter() {
    }

    public static CacheHitCounter getInstance() {
        return instance;
    }

    public void addHit() {
        hits += 1;
    }

    public void addMiss() {
        misses += 1;
    }

    public void addJudgeHit() {
        judgeHits += 1;
    }

    public void addJudgeMiss() {
        judgeMisses += 1;
    }

    public void reset() {
        hits = 0;
        misses = 0;
        judgeHits = 0;
        judgeMisses = 0;
    }
}
