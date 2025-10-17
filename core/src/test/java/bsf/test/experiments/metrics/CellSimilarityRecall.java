package bsf.test.experiments.metrics;

import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CellSimilarityRecall implements IMetric {

    @Override
    public String getName() {
        return "CellSimilarityRecall";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if(expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = new HashSet<>();
        for (Tuple tuple : expected) {
            for (Cell cell : tuple.getCells()) {
                if (!cell.isOID()) {
                    expectedCells.add(cellNormalizer.normalize(cell.getValue().toString()));
                }
            }
        }

        Set<String> resultCells = new HashSet<>();
        for (Tuple tuple : result) {
            if (tuple != null && tuple.getCells() != null) {
                for (Cell cell : tuple.getCells()) {
                    if (!cell.isOID()) {
                        resultCells.add(cellNormalizer.normalize(cell.getValue().toString()));
                    }
                }
            }
        }

        double count = 0.0;
        double totCells = expectedCells.size(); // exclude the OIDs
        double threshold;

        EditDistance editDist = new EditDistance();

        for (String expectedCell : expectedCells) {
            for (String resultCell : resultCells) {
                threshold = expectedCell.length() * 0.1;
                if (editDist.getScoreForCells(expectedCell, resultCell, threshold)) {
                    count++;
                    break;
                }
            }
        }

        //System.out.println("Count: "+count);
        //System.out.println("Total Cells: "+totCells);

        return count / totCells;
    }

}
