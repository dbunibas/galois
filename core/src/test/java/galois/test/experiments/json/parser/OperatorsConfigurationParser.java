package galois.test.experiments.json.parser;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import galois.test.experiments.json.config.OperatorsConfigurationJSON;
import galois.test.experiments.json.parser.operators.ScanConfigurationParser;

public class OperatorsConfigurationParser {
    public static OperatorsConfiguration parseJSON(OperatorsConfigurationJSON json) {
        if (json == null) return getDefault();

        ScanConfiguration scan = ScanConfigurationParser.parse(json.getScan());
        return new OperatorsConfiguration(scan);
    }

    private static OperatorsConfiguration getDefault() {
        ScanConfiguration scan = new ScanConfiguration(new OllamaMistralTableQueryExecutor());
        return new OperatorsConfiguration(scan);
    }
}
