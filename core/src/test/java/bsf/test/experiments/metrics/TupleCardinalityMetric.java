package bsf.test.experiments.metrics;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

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
