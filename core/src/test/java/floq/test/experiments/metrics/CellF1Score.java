package floq.test.experiments.metrics;

import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;

public class CellF1Score implements IMetric {
    @Override
    public String getName() {
        return "F1ScoreMetric";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellPrecision precisionMetric = new CellPrecision();
        Double precision =  precisionMetric.getScore(database, expected, result);

        CellRecall recallMetric = new CellRecall();
        Double recall = recallMetric.getScore(database, expected, result);

        if(precision == null || recall == null || precision + recall == 0)
            return null;

        return 2 * (precision * recall) / (precision + recall);
    }

}