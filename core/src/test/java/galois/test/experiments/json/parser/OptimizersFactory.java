package galois.test.experiments.json.parser;

import galois.optimizer.AggregateConditionsPushdownOptimizer;
import galois.optimizer.AllConditionsPushdownOptimizer;
import galois.optimizer.IOptimizer;

import java.util.Map;

public class OptimizersFactory {
    private static final Map<String, IOptimizerGenerator> optimizersMap = Map.ofEntries(
            Map.entry("AllConditionsPushdownOptimizer", AllConditionsPushdownOptimizer::new),
            Map.entry("AggregateConditionsPushdownOptimizer", AggregateConditionsPushdownOptimizer::new)
            // To add a new metric simply add the entry:
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
