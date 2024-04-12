package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

public class TupleCardinality implements  IMetric{
    @Override
    public String getName() {
        return "TupleCardinality";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        return (double) Math.min(expected.size(), result.size()) /  Math.max(expected.size(), result.size());
    }
}
