package galois.llm.query.utils.cache.db;

import galois.llm.query.IQueryExecutor;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.CacheException;
import com.google.common.hash.Hashing;
import galois.llm.query.utils.cache.ILLMCache;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;

import java.nio.charset.StandardCharsets;

@Slf4j
public class DBCache implements ILLMCache {

    public DBCache() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(Configuration.getInstance().getCacheDbDriver());
        accessConfiguration.setUri(Configuration.getInstance().getCacheDbUri());
        accessConfiguration.setSchemaName("public");
        accessConfiguration.setLogin(Configuration.getInstance().getCacheDbUser());
        accessConfiguration.setPassword(Configuration.getInstance().getCacheDbPassword());

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
                    getLLMModel(queryExecutor),
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
        String executorName = String.format("%s-%s", getProvider(executor), getLLMModel(executor));
        String promptKey = prompt.equals(firstPrompt) ?
                String.format("%s-iter:%d-%s", executorName, iteration, prompt) :
                String.format("%s-fp:%s-iter:%d-%s", executorName, firstPrompt, iteration, prompt);
        return Hashing.crc32().hashString(promptKey, StandardCharsets.UTF_8).toString();
    }

    private String getProvider(IQueryExecutor queryExecutor) {
        return queryExecutor == null ? "LLM-Similarity" : queryExecutor.getClass().getSimpleName();
    }

    private String getLLMModel(IQueryExecutor executor) {
        String simpleName = executor.getClass().getSimpleName();
        if (simpleName.contains("OpenAI")) return Configuration.getInstance().getOpenaiModelName();
        if (simpleName.contains("Togetherai")) return Configuration.getInstance().getTogetheraiModel();
        throw new CacheException("Cannot find model for simple name: " + simpleName);
    }
}


