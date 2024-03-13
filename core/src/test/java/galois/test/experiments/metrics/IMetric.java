package galois.test.experiments.metrics;

import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

public interface IMetric {
    Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result);
}
