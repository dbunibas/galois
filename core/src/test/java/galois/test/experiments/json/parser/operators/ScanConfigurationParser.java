package galois.test.experiments.json.parser.operators;

import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.prompt.EPrompts;
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
            Map.entry("ollama-llama3-key-scan", ScanConfigurationParser::generateOllamaLlama3KeyScanQueryExecutor)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json) {
        return new ScanConfiguration(getExecutor(json));
    }

    private static IQueryExecutor getExecutor(ScanConfigurationJSON json) {
        String name = json.getQueryExecutor();
        if (name == null || name.isEmpty() || !parserMap.containsKey(name))
            throw new IllegalArgumentException("Invalid executor name: " + name + "!");

        IQueryExecutorGenerator generator = parserMap.get(name);
        return generator.create(
                json.getFirstPrompt(),
                json.getIterativePrompt(),
                json.getMaxIterations(),
                json.getAttributesPrompt(),
                json.getPrompt(),
                json.getSql()
        );
    }

    private static Map<String, EPrompts> computePromptsMap() {
        return Arrays.stream(EPrompts.values()).collect(Collectors.toMap(EPrompts::name, Function.identity()));
    }

    private static IQueryExecutor generateOllamaLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt);
    }

    private static IQueryExecutor generateOllamaLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql);
    }

    private static IQueryExecutor generateOllamaLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateOllamaLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    private static IQueryExecutor generateOllamaLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null);
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create(String firstPrompt, String iterativePrompt, int maxIterations,
                              String attributesPrompt, String prompt, String sql);
    }
}
