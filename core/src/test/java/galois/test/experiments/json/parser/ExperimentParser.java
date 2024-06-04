package galois.test.experiments.json.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.optimizer.IOptimization;
import galois.test.experiments.Experiment;
import galois.test.experiments.Query;
import galois.test.experiments.json.ExperimentJSON;
import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.MetricFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ExperimentParser {
    public static Experiment loadAndParseJSON(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL jsonResource = ExperimentParser.class.getResource(fileName);
        ExperimentJSON experimentJSON = mapper.readValue(jsonResource, ExperimentJSON.class);
        return parseJSON(experimentJSON);
    }

    public static Experiment parseJSON(ExperimentJSON json) {
        List<IMetric> metrics = parseMetrics(json.getMetrics());
        List<IOptimization> optimizers = parseOptimizers(json.getOptimizers());
        OperatorsConfiguration operatorsConfiguration = OperatorsConfigurationParser.parseJSON(json.getOperatorsConfig());
        Query query = QueryParser.parseJSON(json.getQuery());
        return new Experiment(json.getName(), json.getDbms(), metrics, optimizers, operatorsConfiguration, query, json.getQueryExecutor());
    }

    private static List<IMetric> parseMetrics(List<String> metrics) {
        return metrics.stream().map(MetricFactory::getMetricByName).toList();
    }

    private static List<IOptimization> parseOptimizers(List<String> optimizers) {
        return optimizers.stream().map(OptimizersFactory::getOptimizerByName).toList();
    }
}
