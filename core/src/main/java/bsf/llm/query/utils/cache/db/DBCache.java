package bsf.llm.query.utils.cache.db;

import bsf.Constants;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.utils.cache.CacheEntry;
import bsf.llm.query.utils.cache.CacheException;
import bsf.llm.query.utils.cache.ILLMCache;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.persistence.relational.AccessConfiguration;

import java.nio.charset.StandardCharsets;

import static bsf.Constants.*;

@Slf4j
public class DBCache implements ILLMCache {

    public DBCache() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(CACHE_DB_DRIVER);
        accessConfiguration.setUri(CACHE_DB_URI);
        accessConfiguration.setSchemaName("public");
        accessConfiguration.setLogin(CACHE_DB_USER);
        accessConfiguration.setPassword(CACHE_DB_PASSWORD);

        DBMSDB dbmsdb = new DBMSDB(accessConfiguration);
        dbmsdb.initDBMS();
    }

    @Override
    public boolean containsQuery(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        try (DaoCache dao = new DaoCache()) {
            dao.connect();
            String cacheKey = getCacheKey(queryExecutor, prompt, iteration, firstPrompt);
            return dao.containsEntry(cacheKey);
        } catch (Exception e) {
            log.error("Cannot execute containsQuery!", e);
            throw new CacheException(e);
        }
    }

    @Override
    public CacheEntry getResponse(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt) {
        try (DaoCache dao = new DaoCache()) {
            dao.connect();
            String cacheKey = getCacheKey(queryExecutor, prompt, iteration, firstPrompt);
            DBCacheEntry cacheEntry = dao.getEntry(cacheKey);
            return cacheEntry == null ?
                    null :
                    new CacheEntry(
                            cacheEntry.getResponse(),
                            cacheEntry.getInputTokens(),
                            cacheEntry.getOutputTokens(),
                            cacheEntry.getTimeMillis(),
                            cacheEntry.getBaseLLMRequestsIncrement()
                    );
        } catch (Exception e) {
            log.error("Cannot execute getResponse!", e);
            throw new CacheException(e);
        }
    }

    @Override
    public void updateCache(String prompt, int iteration, IQueryExecutor queryExecutor, String firstPrompt, String response, double inputTokens, double outputTokens, long timeMillis, int baseLLMRequestsIncrement) {
        try (DaoCache dao = new DaoCache()) {
            dao.connect();
            String cacheKey = getCacheKey(queryExecutor, prompt, iteration, firstPrompt);
            DBCacheEntry dbCacheEntry = new DBCacheEntry(
                    cacheKey,
                    getProvider(queryExecutor),
                    Constants.LLM_MODEL,
                    firstPrompt,
                    prompt,
                    iteration,
                    response,
                    inputTokens,
                    outputTokens,
                    timeMillis,
                    baseLLMRequestsIncrement
            );
            dao.updateEntry(dbCacheEntry);
        } catch (Exception e) {
            log.error("Cannot execute updateCache!", e);
            throw new CacheException(e);
        }
    }

    private String getCacheKey(IQueryExecutor executor, String prompt, int iteration, String firstPrompt) {
        String executorName = String.format("%s-%s", getProvider(executor), Constants.LLM_MODEL);
        String promptKey = prompt.equals(firstPrompt) ?
                String.format("%s-iter:%d-%s", executorName, iteration, prompt) :
                String.format("%s-fp:%s-iter:%d-%s", executorName, firstPrompt, iteration, prompt);
        return Hashing.crc32().hashString(promptKey, StandardCharsets.UTF_8).toString();
    }

    private String getProvider(IQueryExecutor queryExecutor) {
        return queryExecutor == null ? "LLM-Similarity" : queryExecutor.getClass().getSimpleName();
    }
}


