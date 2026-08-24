package galois.test.experiments.metrics;

import lombok.extern.slf4j.Slf4j;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;


@Slf4j
public class TupleCellSimilarityFilteredAttributesPrecision implements IMetric {
    @Override
    public String getName() {
        return "TupleCellSimilarityFilteredAttributesPrecision";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        double precision = TupleCellSimilarityCounter.getInstance().getPrecision(expected, result);
        if (Double.isNaN(precision)) log.error("{} requires TupleCellSimilarityFilteredAttributes to run before it, on the same results!", getName());
        return precision;
    }
}
