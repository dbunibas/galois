package galois.llm.query.utils.cache.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@DatabaseTable(tableName = "entry")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBCacheEntry {
    @DatabaseField(id = true, columnName = "cache_key", columnDefinition = "CHAR(64)")
    private String cacheKey;

    // The shared indexName creates a composite index: llm_cache_provider_model_idx (provider, model)
    @DatabaseField(canBeNull = false, indexName = "llm_cache_provider_model_idx", columnDefinition = "TEXT")
    private String provider;

    @DatabaseField(canBeNull = false, indexName = "llm_cache_provider_model_idx", columnDefinition = "TEXT")
    private String model;

    @DatabaseField(columnDefinition = "TEXT")
    private String firstPrompt;

    @DatabaseField(canBeNull = false, columnDefinition = "TEXT")
    private String prompt;

    @DatabaseField(canBeNull = false, columnDefinition = "INTEGER")
    private int iteration;

    @DatabaseField(columnName = "response", columnDefinition = "TEXT")
    private String response;

    @DatabaseField(columnDefinition = "REAL")
    private Double inputTokens;

    @DatabaseField(columnDefinition = "REAL")
    private Double outputTokens;

    @DatabaseField(columnDefinition = "BIGINT")
    private Long timeMillis;

    @DatabaseField(columnDefinition = "INTEGER")
    private Integer baseLLMRequestsIncrement;
}
