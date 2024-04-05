package galois.test.experiments.metrics;

import java.util.Map;

public class MetricFactory {
    private static final Map<String, IMetricGenerator> metricsMap = Map.ofEntries(
            Map.entry("DummyMetric", DummyMetric::new),
            Map.entry("TupleCardinalityMetric", TupleCardinalityMetric::new)
            // To add a new metric simply add the entry:
            // Map.entry("MetricName", MetricClass::new)
    );

    public static IMetric getMetricByName(String name) {
        if (name == null || name.isEmpty() || !metricsMap.containsKey(name))
            throw new IllegalArgumentException("Invalid metric name!");

        IMetricGenerator metricGenerator = metricsMap.get(name);
        return metricGenerator.create();
    }

    @FunctionalInterface
    private interface IMetricGenerator {
        IMetric create();
    }
}
