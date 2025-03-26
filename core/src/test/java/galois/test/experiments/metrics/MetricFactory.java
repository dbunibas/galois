package galois.test.experiments.metrics;

import java.util.Map;

public class MetricFactory {
    private static final Map<String, IMetricGenerator> metricsMap = Map.ofEntries(
            Map.entry("DummyMetric", DummyMetric::new),
            Map.entry("TupleCardinalityMetric", TupleCardinalityMetric::new),
            Map.entry("CellF1Score", CellF1Score::new),
            Map.entry("CellRecall", CellRecall::new),
            Map.entry("CellPrecision", CellPrecision::new),
            Map.entry("TupleCardinality", TupleCardinality::new),
            Map.entry("TupleConstraint", TupleConstraint::new),
            Map.entry("TupleOrder", TupleOrder::new),
            Map.entry("TupleSimilarityConstraint", TupleSimilarityConstraint::new),
            Map.entry("CellSimilarityPrecision", CellSimilarityPrecision::new),
            Map.entry("CellSimilarityRecall", CellSimilarityRecall::new),
            Map.entry("CellSimilarityF1Score", CellSimilarityF1Score::new),
            Map.entry("CellPrecisionFilteredAttributes", CellPrecisionFilteredAttributes::new),
            Map.entry("CellRecallFilteredAttributes", CellRecallFilteredAttributes::new),
            Map.entry("CellF1ScoreFilteredAttributes", CellF1ScoreFilteredAttributes::new),
            Map.entry("CellSimilarityPrecisionFilteredAttributes", CellSimilarityPrecisionFilteredAttributes::new),
            Map.entry("CellSimilarityRecallFilteredAttributes", CellSimilarityRecallFilteredAttributes::new),
            Map.entry("CellSimilarityF1ScoreFilteredAttributes", CellSimilarityF1ScoreFilteredAttributes::new),
            Map.entry("TupleConstraintFilteredAttributes", TupleConstraintFilteredAttributes::new),
            Map.entry("TupleSimilarityConstraintFilteredAttributes", TupleSimilarityConstraintFilteredAttributes::new)
            // To add a new metric simply add the entry:
            // Map.entry("MetricName", MetricClass::new)
    );

    public static IMetric getMetricByName(String name) {
        if (name == null || name.isEmpty() || !metricsMap.containsKey(name))
            throw new IllegalArgumentException("Invalid metric name: " + name + "!");

        IMetricGenerator metricGenerator = metricsMap.get(name);
        return metricGenerator.create();
    }

    @FunctionalInterface
    private interface IMetricGenerator {
        IMetric create();
    }
}
