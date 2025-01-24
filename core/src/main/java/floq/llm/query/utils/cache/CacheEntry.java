package floq.llm.query.utils.cache;

public record CacheEntry(String response, Double inputTokens, Double outputTokens, Long timeMillis, Integer baseLLMRequestsIncrement) {
}