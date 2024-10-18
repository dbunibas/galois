package floq.test.experiments.metrics;

import engine.model.database.Cell;
import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;

/*
Dummy metric.
Assumes:
- expected size equals to result size;
- attributes from both tuples in the same order;
*/
public class DummyMetric implements IMetric {
    @Override
    public String getName() {
        return "DummyMetric";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        double score = 0.0;
        int totalCells = 0;
        for (int i = 0; i < expected.size(); i++) {
            Tuple expectedTuple = expected.get(i);
            if (i < result.size()) {
                Tuple resultTuple = result.get(i);
                score += getScoreForTuples(expectedTuple, resultTuple);
            }
            totalCells += expectedTuple.size() - 1;
        }
        return score / totalCells;
    }

    private Double getScoreForTuples(Tuple expected, Tuple result) {
        double score = 0.0;
        List<Cell> expectedCells = expected.getCells();
        List<Cell> resultCells = result.getCells();
        for (int i = 0; i < expectedCells.size(); i++) {
            if (expectedCells.get(i).isOID()) continue;
            score += getSCoreForCells(expectedCells.get(i), resultCells.get(i));
        }
        return score;
    }

    private Double getSCoreForCells(Cell expected, Cell result) {
        return expected.getValue().equals(result.getValue()) ? 1.0 : 0.0;
    }
}
