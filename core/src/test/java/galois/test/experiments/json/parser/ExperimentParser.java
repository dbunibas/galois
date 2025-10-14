package galois.test.experiments.json.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.Query;
import galois.test.experiments.json.AttributeJSON;
import galois.test.experiments.json.ExperimentJSON;
import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.MetricFactory;
import galois.utils.attributes.AttributesOverride;
import lombok.Setter;
import speedy.model.database.Attribute;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ExperimentParser {
    @Setter
    private static boolean usePinStrategy = false;
    @Setter
    private static Integer overrideMaxPinPosition = null;

    public static Experiment loadAndParseJSON(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL jsonResource = ExperimentParser.class.getResource(fileName);
        if (jsonResource == null) {
            throw new IllegalArgumentException("Unable to load file " + fileName);
        }
        ExperimentJSON experimentJSON = mapper.readValue(jsonResource, ExperimentJSON.class);
        return parseJSON(experimentJSON);
    }

    public static Experiment parseJSON(ExperimentJSON json) {
        List<IMetric> metrics = parseMetrics(json.getMetrics());
        Query query = QueryParser.parseJSON(json.getQuery());
        OperatorsConfiguration operatorsConfiguration = OperatorsConfigurationParser.parseJSON(json.getOperatorsConfig(), query);
//        List<IOptimizer> optimizers = parseOptimizers(json.getOptimizers(), query);
        List<IOptimizer> optimizers = List.of();
        return new Experiment(
                json.getName(),
                json.getDbms(),
                metrics,
                optimizers,
                operatorsConfiguration,
                query,
                json.getQueryExecutor(),
                parseOverrides(json.getExtraAttributes(), json.getPinnedAttributes(), json.getMaxPinPosition())
        );
    }

    private static List<IMetric> parseMetrics(List<String> metrics) {
        return metrics.stream().map(MetricFactory::getMetricByName).toList();
    }

    private static List<AttributesOverride> parseOverrides(List<AttributeJSON> extraAttributesJSON, List<String> pinnedAttributeNames, Integer maxPinPosition) {
        List<AttributesOverride> overrides = new ArrayList<>();
        List<Attribute> extraAttributes = List.of();
        if (extraAttributesJSON != null) {
            extraAttributes = extraAttributesJSON.stream()
                    .map(a -> new Attribute("", a.getName(), a.getType()))
                    .toList();
        }

        if (extraAttributesJSON != null && !extraAttributesJSON.isEmpty()) {
            if (!usePinStrategy) {
                // Add all the attributes one by one
                for (int i = 0; i < extraAttributes.size(); i++) {
                    overrides.add(new AttributesOverride(extraAttributes.subList(0, i + 1)));
                }
            } else {
                // Add the "pin" experiments configuration from [0, N]
                Integer maxPinOverride = overrideMaxPinPosition != null ? overrideMaxPinPosition : maxPinPosition;
                int maxPin = maxPinOverride != null ? maxPinOverride : extraAttributes.size();
                for (int i = 0; i <= maxPin; i++) {
                    overrides.add(new AttributesOverride(extraAttributes, i));
                }
            }

        }

        return overrides.isEmpty() ? null : overrides;
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
