package galois.test.experiments.json.parser.operators;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.gemini.GeminiKeyQueryExecutor;
import galois.llm.query.gemini.GeminiKeyScanQueryExecutor;
import galois.llm.query.gemini.GeminiNLQueryExecutor;
import galois.llm.query.gemini.GeminiSQLQueryExecutor;
import galois.llm.query.gemini.GeminiTableQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.llm.query.ollama.mistral.OllamaMistralNLQueryExecutor;
import galois.llm.query.openai.*;
import galois.llm.query.togetherai.llama3.*;
import galois.prompt.EPrompts;
import galois.test.experiments.Query;
import galois.test.experiments.json.config.ScanConfigurationJSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ScanConfigurationParser {

    private static ContentRetrieverConfigurationParser contentRetrieverConfigurationParser = new ContentRetrieverConfigurationParser();

    private static final Map<String, IQueryExecutorGenerator> parserMap = Map.ofEntries(
            // Ollama - Llama3
            Map.entry("ollama-llama3-nl", ScanConfigurationParser::generateOllamaLlama3NLQueryExecutor),
            Map.entry("ollama-llama3-sql", ScanConfigurationParser::generateOllamaLlama3SQLQueryExecutor),
            Map.entry("ollama-llama3-table", ScanConfigurationParser::generateOllamaLlama3TableQueryExecutor),
            Map.entry("ollama-llama3-key", ScanConfigurationParser::generateOllamaLlama3KeyQueryExecutor),
            Map.entry("ollama-llama3-key-scan", ScanConfigurationParser::generateOllamaLlama3KeyScanQueryExecutor),
            Map.entry("togetherai-llama3-nl", ScanConfigurationParser::generateTogetheraiLlama3NLQueryExecutor),
            Map.entry("togetherai-llama3-sql", ScanConfigurationParser::generateTogetheraiLlama3SQLQueryExecutor),
            Map.entry("togetherai-llama3-table", ScanConfigurationParser::generateTogetheraiLlama3TableQueryExecutor),
            Map.entry("togetherai-llama3-key", ScanConfigurationParser::generateTogetheraiLlama3KeyQueryExecutor),
            Map.entry("togetherai-llama3-key-scan", ScanConfigurationParser::generateTogetheraiLlama3KeyScanQueryExecutor),
            Map.entry("ollama-mistral-nl", ScanConfigurationParser::generateOllamaMistralNLQueryExecutor),
            Map.entry("open-ai-nl", ScanConfigurationParser::generateOpenAINLQueryExecutor),
            Map.entry("open-ai-sql", ScanConfigurationParser::generateOpenAISQLQueryExecutor),
            Map.entry("open-ai-table", ScanConfigurationParser::generateOpenAITableQueryExecutor),
            Map.entry("open-ai-key-scan", ScanConfigurationParser::generateOpenAIKeyScanQueryExecutor),
            Map.entry("open-ai-key", ScanConfigurationParser::generateOpenAIKeyQueryExecutor),
            Map.entry("gemini-ai-nl", ScanConfigurationParser::generateGeminiNLQueryExecutor),
            Map.entry("gemini-ai-sql", ScanConfigurationParser::generateGeminiSQLQueryExecutor),
            Map.entry("gemini-ai-table", ScanConfigurationParser::generateGeminiTableQueryExecutor),
            Map.entry("gemini-ai-key-scan", ScanConfigurationParser::generateGeminiKeyScanQueryExecutor),
            Map.entry("gemini-ai-key", ScanConfigurationParser::generateGeminiKeyQueryExecutor)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json, Query query) {
        IQueryExecutorGenerator generator = getExecutor(json, query);
        ScanConfiguration.IQueryExecutorFactory factory = (IQueryExecutor base) -> {
            String naturalLanguagePrompt = json.getNaturalLanguagePrompt();
            // Override nl prompt if base is not null - TestRunner .execute set it before
            if (base instanceof INLQueryExectutor nlQueryExecutor) {
                naturalLanguagePrompt = nlQueryExecutor.getNaturalLanguagePrompt();
            }
            ContentRetriever contentRetriever = contentRetrieverConfigurationParser.loadContentRetriever(json.getContentRetriever(), query);
            return generator.create(
                    json.getFirstPrompt(),
                    json.getIterativePrompt(),
                    json.getMaxIterations(),
                    json.getAttributesPrompt(),
                    naturalLanguagePrompt,
                    // Always use query sql - TestRunner .execute set it before
                    query.getSql(),
                    contentRetriever
            );
        };
        return new ScanConfiguration(factory.create(null), factory, json.getNormalizationStrategy(), json.getLlmProbThreshold());
    }

    private static IQueryExecutorGenerator getExecutor(ScanConfigurationJSON json, Query query) {
        String name = json.getQueryExecutor();
        if (name == null || name.isEmpty() || !parserMap.containsKey(name))
            throw new IllegalArgumentException("Invalid executor name: " + name + "!");
        return parserMap.get(name);
    }

    private static Map<String, EPrompts> computePromptsMap() {
        return Arrays.stream(EPrompts.values()).collect(Collectors.toMap(EPrompts::name, Function.identity()));
    }

    private static IQueryExecutor generateOllamaLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }

    private static IQueryExecutor generateOpenAINLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OpenAINLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }
    
    private static IQueryExecutor generateGeminiNLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new GeminiNLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }

    private static IQueryExecutor generateOpenAISQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OpenAISQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }
    
    private static IQueryExecutor generateGeminiSQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new GeminiSQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOpenAITableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OpenAITableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }
    
    private static IQueryExecutor generateGeminiTableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new GeminiTableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLLama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }
    
    private static IQueryExecutor generateGeminiKeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new GeminiKeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOpenAIKeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OpenAIKeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }
    
    private static IQueryExecutor generateGeminiKeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new GeminiKeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOpenAIKeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OpenAIKeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }
    
    private static IQueryExecutor generateOllamaMistralNLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaMistralNLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt); //TODO: Handle content retriever
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create(String firstPrompt, String iterativePrompt, int maxIterations,
                              String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever);
    }
}
