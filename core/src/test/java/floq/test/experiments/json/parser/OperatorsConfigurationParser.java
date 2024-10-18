package floq.test.experiments.json.parser;

import floq.test.experiments.Query;
import floq.test.experiments.json.config.OperatorsConfigurationJSON;
import floq.llm.algebra.config.OperatorsConfiguration;
import floq.llm.algebra.config.ScanConfiguration;
import floq.llm.query.ollama.llama3.OllamaLlama3KeyScanQueryExecutor;
import floq.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import floq.test.experiments.json.parser.operators.ScanConfigurationParser;

public class OperatorsConfigurationParser {
    public static OperatorsConfiguration parseJSON(OperatorsConfigurationJSON json, Query query) {
        if (json == null) return getDefault();

        ScanConfiguration scan = ScanConfigurationParser.parse(json.getScan(), query);
        return new OperatorsConfiguration(scan);
    }

    public static OperatorsConfiguration getDefault() {
        ScanConfiguration scan = new ScanConfiguration(new OllamaLlama3KeyScanQueryExecutor(), (ignored) -> new OllamaLlama3KeyScanQueryExecutor(), null);
        return new OperatorsConfiguration(scan);
    }
}
