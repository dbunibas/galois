package galois.test.experiments.json.parser.operators;

import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.llm.query.ollama.mistral.OllamaMistralNLQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLLama3KeyQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3KeyScanQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3TableQueryExecutor;
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
            Map.entry("ollama-mistral-nl", ScanConfigurationParser::generateOllamaMistralNLQueryExecutor)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json, Query query) {
        IQueryExecutorGenerator generator = getExecutor(json, query);
        ScanConfiguration.IQueryExecutorFactory factory = () -> generator.create(
                json.getFirstPrompt(),
                json.getIterativePrompt(),
                json.getMaxIterations(),
                json.getAttributesPrompt(),
                json.getNaturalLanguagePrompt(),
                json.getSql() == null || json.getSql().isBlank() ?
                        query.getSql() :
                        json.getSql()
        );
        return new ScanConfiguration(factory.create(), factory, json.getNormalizationStrategy());
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

    private static IQueryExecutor generateOllamaLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt);
    }

    private static IQueryExecutor generateTogetheraiLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt);
    }

    private static IQueryExecutor generateOllamaLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql);
    }

    private static IQueryExecutor generateTogetheraiLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql);
    }

    private static IQueryExecutor generateOllamaLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateTogetheraiLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateOllamaLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLLama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateOllamaLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateOllamaMistralNLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaMistralNLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt);
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create(String firstPrompt, String iterativePrompt, int maxIterations,
                              String attributesPrompt, String prompt, String sql);
    }
}
