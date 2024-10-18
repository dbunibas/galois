package floq.test.experiments.metrics;

import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;

public interface IMetric {
    String getName();

    Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result);
}
