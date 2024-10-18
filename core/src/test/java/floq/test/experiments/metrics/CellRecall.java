package floq.test.experiments.metrics;

import engine.model.database.Cell;
import engine.model.database.IDatabase;
import engine.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class CellRecall implements IMetric {

    @Override
    public String getName() {
        return "CellRecall";
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
        Set<String> commonCells = new HashSet<>(expectedCells);
        commonCells.retainAll(resultCells);

        double count = commonCells.size(); // Number of common cells
        double totalExpectedCells = expectedCells.size(); // Exclude the OIDs

        return count / totalExpectedCells;
    }
}
