package bsf.test.experiments.metrics;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

import java.util.List;

/*
Dummy metric.
Assumes:
- expected size equals to result size;
- attributes from both tuples in the same order;
*/
public class CellSimilarityF1ScoreFilteredAttributes implements IMetric {
    @Override
    public String getName() {
        return "CellSimilarityF1ScoreFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellSimilarityPrecisionFilteredAttributes precisionMetric = new CellSimilarityPrecisionFilteredAttributes();
        Double precision =  precisionMetric.getScore(database, expected, result);

        CellSimilarityRecallFilteredAttributes recallMetric = new CellSimilarityRecallFilteredAttributes();
        Double recall = recallMetric.getScore(database, expected, result);

        if(precision == null || recall == null || precision + recall == 0) return 0.0;

        return 2 * (precision * recall) / (precision + recall);
    }

}