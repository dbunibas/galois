package bsf.test.experiments.json.parser;

import bsf.llm.algebra.config.OperatorsConfiguration;
import bsf.llm.algebra.config.ScanConfiguration;
import bsf.llm.query.ollama.llama3.OllamaLlama3KeyScanQueryExecutor;
import bsf.test.experiments.Query;
import bsf.test.experiments.json.config.OperatorsConfigurationJSON;
import bsf.test.experiments.json.parser.operators.ScanConfigurationParser;

public class OperatorsConfigurationParser {
    public static OperatorsConfiguration parseJSON(OperatorsConfigurationJSON json, Query query) {
        if (json == null) return getDefault();

        ScanConfiguration scan = ScanConfigurationParser.parse(json.getScan(), query);
        return new OperatorsConfiguration(scan);
    }

    public static OperatorsConfiguration getDefault() {
        ScanConfiguration scan = new ScanConfiguration(new OllamaLlama3KeyScanQueryExecutor(), (ignored) -> new OllamaLlama3KeyScanQueryExecutor(), null, null);
        return new OperatorsConfiguration(scan);
    }
}
