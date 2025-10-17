package bsf.test.experiments.json.parser;

import bsf.optimizer.AggregateConditionsPushdownOptimizer;
import bsf.optimizer.AllConditionsPushdownOptimizer;
import bsf.optimizer.IOptimizer;

import java.util.Map;

public class OptimizersFactory {
    private static final Map<String, IOptimizerGenerator> optimizersMap = Map.ofEntries(
            Map.entry("AllConditionsPushdownOptimizer", () -> new AllConditionsPushdownOptimizer(true)),
            Map.entry("AggregateConditionsPushdownOptimizer", () -> new AggregateConditionsPushdownOptimizer(true)),
            Map.entry("AllConditionsPushdownOptimizer-WithFilter", () -> new AllConditionsPushdownOptimizer(false)),
            Map.entry("AggregateConditionsPushdownOptimizer-WithFilter", () -> new AggregateConditionsPushdownOptimizer(false))
            // To add a new optimizer simply add the entry:
            // Map.entry("OptimizerName", OptimizerClass::new)
    );

    public static IOptimizer getOptimizerByName(String name) {
        if (name == null || name.isEmpty() || !optimizersMap.containsKey(name))
            throw new IllegalArgumentException("Invalid optimizer name: " + name + "!");

        IOptimizerGenerator optimizerGenerator = optimizersMap.get(name);
        return optimizerGenerator.create();
    }

    @FunctionalInterface
    private interface IOptimizerGenerator {
        IOptimizer create();
    }
}
