package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;

public class CellF1ScoreFilteredAttributes implements IMetric {
    @Override
    public String getName() {
        return "F1ScoreMetricFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellPrecisionFilteredAttributes precisionMetric = new CellPrecisionFilteredAttributes();
        Double precision =  precisionMetric.getScore(database, expected, result);

        CellRecallFilteredAttributes recallMetric = new CellRecallFilteredAttributes();
        Double recall = recallMetric.getScore(database, expected, result);

        if(precision == null || recall == null || precision + recall == 0) return null;

        return 2 * (precision * recall) / (precision + recall);
    }

}