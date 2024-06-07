package galois.test.experiments.json.parser;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.ollama.llama3.OllamaLlama3KeyScanQueryExecutor;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import galois.test.experiments.Query;
import galois.test.experiments.json.config.OperatorsConfigurationJSON;
import galois.test.experiments.json.parser.operators.ScanConfigurationParser;

public class OperatorsConfigurationParser {
    public static OperatorsConfiguration parseJSON(OperatorsConfigurationJSON json, Query query) {
        if (json == null) return getDefault();

        ScanConfiguration scan = ScanConfigurationParser.parse(json.getScan(), query);
        return new OperatorsConfiguration(scan);
    }

    public static OperatorsConfiguration getDefault() {
        ScanConfiguration scan = new ScanConfiguration(new OllamaLlama3KeyScanQueryExecutor());
        return new OperatorsConfiguration(scan);
    }
}
