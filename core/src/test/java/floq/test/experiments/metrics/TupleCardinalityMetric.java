package floq.test.experiments.metrics;

import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;

public class TupleCardinalityMetric implements IMetric {
    @Override
    public String getName() {
        return "TupleCardinalityMetric";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        return expected.size() == result.size() ? 1.0 : 0.0;
    }
}
