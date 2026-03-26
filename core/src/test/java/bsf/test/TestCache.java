package bsf.test;

import bsf.llm.query.gemini.GeminiKeyScanQueryExecutor;
import bsf.llm.query.openai.OpenAIKeyScanQueryExecutor;
import bsf.llm.query.togetherai.llama3.TogetheraiLlama3KeyScanQueryExecutor;
import bsf.llm.query.utils.cache.CacheEntry;
import bsf.llm.query.utils.cache.LLMCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCache {
    @Test
    public void testDoNotContains() {
        TogetheraiLlama3KeyScanQueryExecutor executor = new TogetheraiLlama3KeyScanQueryExecutor();

        String prompt = "testDoNotContains";
        String firstPrompt = "firstPrompt-testDoNotContains";
        int iteration = 0;

        boolean result = LLMCache.getInstance().containsQuery(prompt, iteration, executor, firstPrompt);
        assertFalse(result);
    }

    @Test
    public void testNullForEmpty() {
        TogetheraiLlama3KeyScanQueryExecutor executor = new TogetheraiLlama3KeyScanQueryExecutor();

        String prompt = "testNullForEmpty";
        String firstPrompt = "firstPrompt-testNullForEmpty";
        int iteration = 0;

        CacheEntry result = LLMCache.getInstance().getResponse(prompt, iteration, executor, firstPrompt);
        assertNull(result);
    }

    @Test
    public void testUpdateCache() {
        TogetheraiLlama3KeyScanQueryExecutor executor = new TogetheraiLlama3KeyScanQueryExecutor();

        String prompt = "testUpdateCache";
        String firstPrompt = "firstPrompt-testUpdateCache";
        int iteration = 0;

        String response = "testUpdateCache-response";
        int value = 1;
        LLMCache.getInstance().updateCache(prompt, iteration, executor, firstPrompt, response, value, value, value, value);

        boolean contains = LLMCache.getInstance().containsQuery(prompt, iteration, executor, firstPrompt);
        assertTrue(contains);

        CacheEntry result = LLMCache.getInstance().getResponse(prompt, iteration, executor, firstPrompt);
        assertNotNull(result);
        assertEquals(response, result.response());
        assertEquals(value, result.inputTokens());
        assertEquals(value, result.outputTokens());
        assertEquals(value, result.timeMillis());
        assertEquals(value, result.baseLLMRequestsIncrement());
    }

    @Test
    public void testUpdateCacheOpenAI() {
        OpenAIKeyScanQueryExecutor executor = new OpenAIKeyScanQueryExecutor();

        String prompt = "testUpdateCache";
        String firstPrompt = "firstPrompt-testUpdateCache";
        int iteration = 0;

        String response = "testUpdateCache-response";
        int value = 1;
        LLMCache.getInstance().updateCache(prompt, iteration, executor, firstPrompt, response, value, value, value, value);

        boolean contains = LLMCache.getInstance().containsQuery(prompt, iteration, executor, firstPrompt);
        assertTrue(contains);

        CacheEntry result = LLMCache.getInstance().getResponse(prompt, iteration, executor, firstPrompt);
        assertNotNull(result);
        assertEquals(response, result.response());
        assertEquals(value, result.inputTokens());
        assertEquals(value, result.outputTokens());
        assertEquals(value, result.timeMillis());
        assertEquals(value, result.baseLLMRequestsIncrement());
    }

    @Test
    public void testUpdateCacheGemini() {
        GeminiKeyScanQueryExecutor executor = new GeminiKeyScanQueryExecutor();

        String prompt = "testUpdateCache";
        String firstPrompt = "firstPrompt-testUpdateCache";
        int iteration = 0;

        String response = "testUpdateCache-response";
        int value = 1;
        LLMCache.getInstance().updateCache(prompt, iteration, executor, firstPrompt, response, value, value, value, value);

        boolean contains = LLMCache.getInstance().containsQuery(prompt, iteration, executor, firstPrompt);
        assertTrue(contains);

        CacheEntry result = LLMCache.getInstance().getResponse(prompt, iteration, executor, firstPrompt);
        assertNotNull(result);
        assertEquals(response, result.response());
        assertEquals(value, result.inputTokens());
        assertEquals(value, result.outputTokens());
        assertEquals(value, result.timeMillis());
        assertEquals(value, result.baseLLMRequestsIncrement());
    }
}
