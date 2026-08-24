package galois.test.experiments.metrics;

import lombok.extern.slf4j.Slf4j;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

@Slf4j
public class TupleCellSimilarityFilteredAttributesRecall implements IMetric {
    @Override
    public String getName() {
        return "TupleCellSimilarityFilteredAttributesRecall";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        double recall = TupleCellSimilarityCounter.getInstance().getRecall(expected, result);
        if (Double.isNaN(recall)) log.error("{} requires TupleCellSimilarityFilteredAttributes to run before it, on the same results!", getName());
        return recall;
    }
}
