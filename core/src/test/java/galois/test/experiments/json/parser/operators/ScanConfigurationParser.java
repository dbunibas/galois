package galois.test.experiments.json.parser.operators;

import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.OllamaMistralQAQueryExecutor;
import galois.llm.query.ollama.OllamaMistralTableQueryExecutor;
import galois.test.experiments.json.config.ScanConfigurationJSON;

import java.util.Map;

public class ScanConfigurationParser {
    private static final Map<String, IQueryExecutorGenerator> parserMap = Map.ofEntries(
            Map.entry(OllamaMistralQAQueryExecutor.class.getSimpleName(), OllamaMistralQAQueryExecutor::new),
            Map.entry(OllamaMistralTableQueryExecutor.class.getSimpleName(), OllamaMistralTableQueryExecutor::new)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json) {
        return new ScanConfiguration(getExecutor(json.getQueryExecutor()));
    }

    private static IQueryExecutor getExecutor(String name) {
        if (name == null || name.isEmpty() || !parserMap.containsKey(name))
            throw new IllegalArgumentException("Invalid executor name!");

        IQueryExecutorGenerator generator = parserMap.get(name);
        return generator.create();
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create();
    }
}