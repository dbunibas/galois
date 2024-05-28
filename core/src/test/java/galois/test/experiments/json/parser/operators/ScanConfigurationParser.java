package galois.test.experiments.json.parser.operators;

import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.llamacpp.LlamaCppKeyAttributesQueryExecutor;
import galois.llm.query.ollama.llama3.OllamaLLama3KeyQueryExecutor;
import galois.llm.query.ollama.llama3.OllamaLLama3KeyScanQueryExecutor;
import galois.llm.query.ollama.llama3.OllamaLlama3TableQueryExecutor;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import galois.llm.query.outlines.OutlinesKeyAttributesQueryExecutor;
import galois.llm.query.outlines.OutlinesKeyValueQueryExecutor;
import galois.test.experiments.json.config.ScanConfigurationJSON;

import java.util.Map;

public class ScanConfigurationParser {
    private static final Map<String, IQueryExecutorGenerator> parserMap = Map.ofEntries(
            Map.entry(OllamaMistralTableQueryExecutor.class.getSimpleName(), OllamaMistralTableQueryExecutor::new),

            Map.entry(OllamaLlama3TableQueryExecutor.class.getSimpleName(), OllamaLlama3TableQueryExecutor::new),
            Map.entry(OllamaLLama3KeyScanQueryExecutor.class.getSimpleName(), OllamaLLama3KeyScanQueryExecutor::new),
            Map.entry(OllamaLLama3KeyQueryExecutor.class.getSimpleName(), OllamaLLama3KeyQueryExecutor::new),

            Map.entry(OutlinesKeyAttributesQueryExecutor.class.getSimpleName(), OutlinesKeyAttributesQueryExecutor::new),
            Map.entry(OutlinesKeyValueQueryExecutor.class.getSimpleName(), OutlinesKeyValueQueryExecutor::new),

            Map.entry(LlamaCppKeyAttributesQueryExecutor.class.getSimpleName(), LlamaCppKeyAttributesQueryExecutor::new)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json) {
        return new ScanConfiguration(getExecutor(json.getQueryExecutor()));
    }

    private static IQueryExecutor getExecutor(String name) {
        if (name == null || name.isEmpty() || !parserMap.containsKey(name))
            throw new IllegalArgumentException("Invalid executor name: " + name + "!");

        IQueryExecutorGenerator generator = parserMap.get(name);
        return generator.create();
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create();
    }
}
