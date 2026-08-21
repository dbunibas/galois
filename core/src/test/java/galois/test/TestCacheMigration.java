package galois.test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.openai.OpenAIKeyQueryExecutor;
import galois.llm.query.openai.OpenAIKeyScanQueryExecutor;
import galois.llm.query.openai.OpenAINLQueryExecutor;
import galois.llm.query.openai.OpenAISQLQueryExecutor;
import galois.llm.query.openai.OpenAITableQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLLama3KeyQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3KeyScanQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3TableQueryExecutor;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.CacheException;
import galois.llm.query.utils.cache.db.DBCache;
import galois.llm.query.utils.cache.db.DBCacheEntry;
import galois.llm.query.utils.cache.db.DaoCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestCacheMigration {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CACHE_FOLDER = "/Users/dario/ricerca/galois/code/cache-migration/cache";

    // The entries are persisted in batches to keep both the transactions and the memory footprint bounded
    private static final int BATCH_SIZE = 1000;

    private static final Map<String, IQueryExecutor> EXECUTOR_MAP = Map.of(
            "TogetheraiLlama3NLQueryExecutor", new TogetheraiLlama3NLQueryExecutor(null),
            "TogetheraiLlama3SQLQueryExecutor", new TogetheraiLlama3SQLQueryExecutor(null),
            "TogetheraiLlama3TableQueryExecutor", new TogetheraiLlama3TableQueryExecutor(),
            "TogetheraiLLama3KeyQueryExecutor", new TogetheraiLLama3KeyQueryExecutor(),
            "TogetheraiLlama3KeyScanQueryExecutor", new TogetheraiLlama3KeyScanQueryExecutor(),

            "OpenAINLQueryExecutor", new OpenAINLQueryExecutor(null),
            "OpenAISQLQueryExecutor", new OpenAISQLQueryExecutor(null),
            "OpenAITableQueryExecutor", new OpenAITableQueryExecutor(),
            "OpenAIKeyQueryExecutor", new OpenAIKeyQueryExecutor(),
            "OpenAIKeyScanQueryExecutor", new OpenAIKeyScanQueryExecutor()
    );

    private static final Map<String, String> MODEL_MAP = Map.ofEntries(
            Map.entry("Meta-Llama-3.1-8B-Instruct-Turbo", TogetherAIConstants.MODEL_LLAMA3_1_8B),
            Map.entry("Llama-3.3-70B-Instruct-Turbo", TogetherAIConstants.MODEL_LLAMA3_3_70B),
            Map.entry("Llama-4-Scout-17B-16E-Instruct", TogetherAIConstants.MODEL_LLAMA4_SCOUT),
            Map.entry("DeepSeek-R1-Distill-Llama-70B-free", TogetherAIConstants.MODEL_DEEPSEEK_R1_DISTIL_LLAMA_70B),
            Map.entry("Qwen2.5-7B-Instruct-Turbo", TogetherAIConstants.MODEL_QWEN_2_5_7B),
            Map.entry("Qwen3-235B-A22B-fp8-tput", TogetherAIConstants.MODEL_QWEN3_235B),
            Map.entry("Mistral-7B-Instruct-v0.3", TogetherAIConstants.MODEL_MISTRAL_0_3_7B),
            Map.entry("gemma-2-9b-it", TogetherAIConstants.MODEL_GEMMA_2_9B),
            Map.entry("Kimi-K2-Instruct-0905", TogetherAIConstants.MODEL_KIMI_K2),

            Map.entry("gpt-4.1", "gpt-4.1"),
            Map.entry("GPT_4.1_mini", "gpt-4.1-mini"),
            Map.entry("gpt-4.1-mini", "gpt-4.1-mini"),
            Map.entry("GPT_4.1_nano", "gpt-4.1-nano"),
            Map.entry("GPT_4_O", "gpt-4o"),
            Map.entry("GPT_4o_mini", "gpt-4o-mini")
    );

    // File cache names are "cache-{queryExecutorName}-{llmProvider}.json"
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^cache-(?<executor>.+)-(?<provider>[^-]+)$");
    // File cache keys are "iter:{iteration}-{prompt}" or "fp:{firstPrompt}-iter:{iteration}-{prompt}"
    private static final Pattern KEY_PATTERN = Pattern.compile("^(?:fp:(?<firstPrompt>.*?)-)?iter:(?<iteration>\\d+)-(?<prompt>.*)$", Pattern.DOTALL);
    private static final String SIMILARITY_EXECUTOR_NAME = "LLM-Similarity";

    @Test
    public void testCanLoadCaches() {
        Collection<File> caches = loadFileCaches(CACHE_FOLDER);

        for (File cache : caches) {
            log.debug("{}", cache.getName());
            assertDoesNotThrow(() -> forEachEntry(cache, (key, entry) -> {
            }));
        }
    }

    @Test
    public void testCacheData() {
        Collection<File> caches = loadFileCaches(CACHE_FOLDER);

        for (File file : caches) {
            log.debug("{}", file.getName());

            IQueryExecutor queryExecutor = getQueryExecutor(file);
            assertTrue(queryExecutor == null || EXECUTOR_MAP.containsValue(queryExecutor));

            String model = getModel(file);
            assertNotNull(model);

            forEachEntry(file, (key, entry) -> {
                String prompt = getPrompt(key);
                assertNotNull(prompt);
                int iteration = getIteration(key);
                assertTrue(iteration >= 0 && iteration <= 150);
                String firstPrompt = getFirstPrompt(key);
                assertTrue(firstPrompt == null || !firstPrompt.trim().isBlank());
            });
        }
    }

    @Test
    public void testMigrateCaches() throws Exception {
        new DBCache(); // Initializes the DBMS of the cache
        Collection<File> caches = loadFileCaches(CACHE_FOLDER);

        try (DaoCache dao = new DaoCache()) {
            dao.connect();

            for (File file : caches) {
                log.info("Migrating the cache {}", file.getName());
                IQueryExecutor queryExecutor = getQueryExecutor(file);
                String model = getModel(file);

                List<DBCacheEntry> batch = new ArrayList<>(BATCH_SIZE);
                forEachEntry(file, (key, entry) -> {
                    batch.add(getMigrationEntry(key, entry, queryExecutor, model));
                    if (batch.size() == BATCH_SIZE) updateEntries(dao, batch);
                });
                updateEntries(dao, batch);
                log.info("Migrated the cache {}", file.getName());
            }
        }
    }

    private DBCacheEntry getMigrationEntry(String key, CacheEntry entry, IQueryExecutor queryExecutor, String model) {
        String prompt = getPrompt(key);
        String firstPrompt = getFirstPrompt(key);

        return DBCache.getMigrationEntry(
                prompt,
                getIteration(key),
                queryExecutor,
                model,
                firstPrompt == null ? prompt : firstPrompt,
                entry.response(),
                entry.inputTokens(),
                entry.outputTokens(),
                entry.timeMillis(),
                entry.baseLLMRequestsIncrement()
        );
    }

    private void updateEntries(DaoCache dao, List<DBCacheEntry> entries) {
        if (entries.isEmpty()) return;

        try {
            dao.updateEntries(entries);
            entries.clear();
        } catch (Exception e) {
            throw new CacheException("Cannot migrate the cache entries!", e);
        }
    }

    private Collection<File> loadFileCaches(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) throw new IllegalArgumentException("Invalid absolute folder path!");
        return FileUtils.listFiles(folder, new String[]{"json"}, true);
    }

    // The caches are streamed entry by entry: the biggest ones do not fit in memory
    private void forEachEntry(File cache, BiConsumer<String, CacheEntry> consumer) {
        if (!cache.exists()) throw new IllegalArgumentException("Invalid absolute cache path!");

        try (JsonParser parser = MAPPER.getFactory().createParser(cache)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) throw new CacheException("Cache is not a JSON object!");

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String key = parser.currentName();
                parser.nextToken(); // Moves to the value of the entry
                consumer.accept(key, MAPPER.readValue(parser, CacheEntry.class));
            }
        } catch (IOException e) {
            throw new CacheException("Cannot load cache!", e);
        }
    }

    private IQueryExecutor getQueryExecutor(File file) {
        String executorName = getExecutorName(file);
        if (SIMILARITY_EXECUTOR_NAME.equals(executorName)) return null;

        IQueryExecutor queryExecutor = EXECUTOR_MAP.get(executorName);
        if (queryExecutor == null) throw new IllegalArgumentException("Unknown query executor: " + executorName);
        return queryExecutor;
    }

    private String getExecutorName(File file) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        Matcher matcher = FILE_NAME_PATTERN.matcher(baseName);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid cache file name: " + file.getName());
        return matcher.group("executor");
    }

    private String getModel(File file) {
        if (getQueryExecutor(file) == null) return TogetherAIConstants.MODEL_LLAMA3_3_70B;

        File folder = file.getParentFile();
        if (folder == null) throw new IllegalArgumentException("Cache file without a model folder: " + file);

        String model = MODEL_MAP.get(folder.getName());
        if (model == null) throw new IllegalArgumentException("Unknown model folder: " + folder.getName());
        return model;
    }

    private String getPrompt(String key) {
        return matchKey(key).group("prompt");
    }

    private int getIteration(String key) {
        return Integer.parseInt(matchKey(key).group("iteration"));
    }

    private String getFirstPrompt(String key) {
        return matchKey(key).group("firstPrompt");
    }

    private Matcher matchKey(String key) {
        Matcher matcher = KEY_PATTERN.matcher(key);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid cache key: " + key);
        return matcher;
    }
}
