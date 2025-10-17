package bsf.test.experiments.metrics;

import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class CellPrecision implements IMetric {

    @Override
    public String getName() {
        return "CellPrecision";
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

        // Calculate the intersection size directly
        Set<String> commonCells = new HashSet<>(resultCells);
        commonCells.retainAll(expectedCells);

        int totalCells = resultCells.size(); // excluded the OIDs

        return (double) commonCells.size() / totalCells;
    }

}
