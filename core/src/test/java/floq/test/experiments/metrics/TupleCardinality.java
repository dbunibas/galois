package floq.test.experiments.metrics;

import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;

public class TupleCardinality implements  IMetric{
    @Override
    public String getName() {
        return "TupleCardinality";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if(expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        return (double) Math.min(expected.size(), result.size()) /  Math.max(expected.size(), result.size());
    }
}