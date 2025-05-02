package galois.llm.query.utils.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import galois.Constants;
import galois.llm.query.IQueryExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static galois.Constants.CACHE_DIR;
import static galois.Constants.CACHE_ENABLED;

@Slf4j
public class LLMCache {
    private static final TypeReference<Map<String, CacheEntry>> MAP_REFERENCE = new TypeReference<>() {
    };

    @Getter
    private static final LLMCache instance = new LLMCache();

    private final ObjectMapper mapper = new ObjectMapper();

    private String cacheName = null;
    private Map<String, CacheEntry> cache = null;

    private LLMCache() {
    }
    
    public String getFileName(IQueryExecutor queryExecutor) {
        String queryExecutorName = null;
        if (queryExecutor == null) {
            queryExecutorName = "LLM-Similarity";
        } else {
            queryExecutorName = queryExecutor.getClass().getSimpleName();
        }
        return String.format("%s/cache-%s-%s.json", CACHE_DIR, queryExecutorName, Constants.LLM_MODEL);
    }

    public boolean containsQuery(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!CACHE_ENABLED)
            return false;

        Map<String, CacheEntry> cache = loadCache(queryExecutor);
        String key = getKey(prompt, iteration, firstPrompt);
        return cache.containsKey(key);
    }

    public CacheEntry getResponse(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        if (!CACHE_ENABLED)
            throw new UnsupportedOperationException("Cannot use cache when it is disabled!");

        Map<String, CacheEntry> cache = loadCache(queryExecutor);
        String key = getKey(prompt, iteration, firstPrompt);
        return cache.get(key);
    }

    public void updateCache(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt, String response, double inputTokens, double outputTokens, long timeMillis, int baseLLMRequestsIncrement) {
        if (!CACHE_ENABLED) return;
        Map<String, CacheEntry> cache = loadCache(queryExecutor);
        String key = getKey(prompt, iteration, firstPrompt);
        cache.put(key, new CacheEntry(response, inputTokens, outputTokens, timeMillis, baseLLMRequestsIncrement));

        writeCache(cache, queryExecutor);
    }

    private Map<String, CacheEntry> loadCache(IQueryExecutor queryExecutor) {
        String fileName = getFileName(queryExecutor);
        if (cache != null && fileName.equals(cacheName)) return cache;
        File file = new File(fileName);
        if (!file.exists())
            return new HashMap<>();

        try {
            cache = mapper.readValue(file, MAP_REFERENCE);
            cacheName = fileName;
            return cache;
        } catch (IOException e) {
            throw new CacheException("Cannot load cache!", e);
        }
    }

    private String getKey(String prompt, int iteration, String firstPrompt) {
        return prompt.equals(firstPrompt) ?
                String.format("iter:%d-%s", iteration, prompt) :
                String.format("fp:%s-iter:%d-%s", firstPrompt, iteration, prompt);
    }

    private void writeCache(Map<String, CacheEntry> cache, IQueryExecutor queryExecutor) {
        String executorName = "";
        if (queryExecutor == null) {
            executorName = "LLM-Similarity";
        } else {
            executorName = queryExecutor.getClass().getSimpleName();
        }
        String fileName = getFileName(queryExecutor);
        File file = new File(fileName);
        String updatedFileName = String.format("%s/cache-%s-updated.json", CACHE_DIR, executorName);
        File updatedFile = new File(updatedFileName);

        try {
            if (!file.exists()) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, cache);
                return;
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(updatedFile, cache);
            if (!file.delete() || !updatedFile.renameTo(file)) {
                throw new CacheException("Cannot update cache!");
            }
        } catch (IOException e) {
            if (updatedFile.exists()) updatedFile.delete();
            throw new CacheException("Cannot save cache!", e);
        }
    }
}
