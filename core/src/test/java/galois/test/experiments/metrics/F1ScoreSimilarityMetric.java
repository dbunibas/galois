package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

/*
Dummy metric.
Assumes:
- expected size equals to result size;
- attributes from both tuples in the same order;
*/
public class F1ScoreSimilarityMetric implements IMetric {
    @Override
    public String getName() {
        return "F1ScoreSimilarityMetric";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellSimilarityPrecision precisionMetric = new CellSimilarityPrecision();
        Double precision =  precisionMetric.getScore(database, expected, result);

        CellSimilarityRecall recallMetric = new CellSimilarityRecall();
        Double recall = recallMetric.getScore(database, expected, result);

        if(precision == null || recall == null || precision + recall == 0)
            return null;

        return 2 * (precision * recall) / (precision + recall);
    }

}