package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

public interface IMetric {
    String getName();

    Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result);
}
