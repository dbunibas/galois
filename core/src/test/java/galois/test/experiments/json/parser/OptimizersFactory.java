package galois.test.experiments.json.parser;

import galois.optimizer.AllConditionPushdownOptimizer;
import galois.optimizer.IOptimization;

import java.util.Map;

public class OptimizersFactory {
    private static final Map<String, IOptimizerGenerator> optimizersMap = Map.ofEntries(
            Map.entry("AllConditionPushdownOptimizer", AllConditionPushdownOptimizer::new)
            // To add a new metric simply add the entry:
            // Map.entry("OptimizerName", OptimizerClass::new)
    );

    public static IOptimization getOptimizerByName(String name) {
        if (name == null || name.isEmpty() || !optimizersMap.containsKey(name))
            throw new IllegalArgumentException("Invalid metric name: " + name + "!");

        IOptimizerGenerator optimizerGenerator = optimizersMap.get(name);
        return optimizerGenerator.create();
    }

    @FunctionalInterface
    private interface IOptimizerGenerator {
        IOptimization create();
    }
}
