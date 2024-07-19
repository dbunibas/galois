package galois.test.experiments.json.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.Query;
import galois.test.experiments.json.ExperimentJSON;
import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.MetricFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
        Query query = QueryParser.parseJSON(json.getQuery());
        OperatorsConfiguration operatorsConfiguration = OperatorsConfigurationParser.parseJSON(json.getOperatorsConfig(), query);
        List<IOptimizer> optimizers = parseOptimizers(json.getOptimizers(), query);
        return new Experiment(json.getName(), json.getDbms(), metrics, optimizers, operatorsConfiguration, query, json.getQueryExecutor());
    }

    private static List<IMetric> parseMetrics(List<String> metrics) {
        return metrics.stream().map(MetricFactory::getMetricByName).toList();
    }

    private static List<IOptimizer> parseOptimizers(List<String> optimizers, Query query) {
        if (optimizers == null || optimizers.isEmpty()) {
            return List.of();
        }

        List<IOptimizer> parsedOptimizers = new ArrayList<>();

        // TODO: Refactor by changing the OptimizersFactory signature
        for (String optimizer : optimizers) {
            
            if (optimizer.equals("SingleConditionsOptimizerFactory")) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(query.getSql());
                for (int i = 0; i < parserWhere.getExpressions().size(); i++) {
                    parsedOptimizers.add(new IndexedConditionPushdownOptimizer(i, true));
                }
                continue;
            }
            if (optimizer.equals("SingleConditionsOptimizerFactory-WithFilter")) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(query.getSql());
                for (int i = 0; i < parserWhere.getExpressions().size(); i++) {
                    parsedOptimizers.add(new IndexedConditionPushdownOptimizer(i, false));
                }
                continue;
            }

            parsedOptimizers.add(OptimizersFactory.getOptimizerByName(optimizer));
        }

        return parsedOptimizers;
    }
}
