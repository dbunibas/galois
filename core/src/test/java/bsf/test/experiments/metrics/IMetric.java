package bsf.test.experiments.metrics;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

import java.util.List;

public interface IMetric {
    String getName();

    Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result);
}
