package galois.test.experiments.metrics;

import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class CellPrecisionFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "CellPrecisionFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if(expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = new HashSet<>();
        Set<String> expectedAttributes = new HashSet<>();
        computeExpectedCells(expected, expectedAttributes, expectedCells, cellNormalizer);

        Set<String> resultCells = new HashSet<>();
        computeResultCells(result, expectedAttributes, resultCells, cellNormalizer);

        // Calculate the intersection size directly
        Set<String> commonCells = new HashSet<>(resultCells);
        commonCells.retainAll(expectedCells);

        int totalCells = resultCells.size(); // excluded the OIDs

        return (double) commonCells.size() / totalCells;
    }

    public static void computeResultCells(List<Tuple> result, Set<String> expectedAttributes, Set<String> resultCells, CellNormalizer cellNormalizer) {
        for (Tuple tuple : result) {
            if (tuple != null && tuple.getCells() != null) {
                for (Cell cell : tuple.getCells()) {
                    if (!cell.isOID() && expectedAttributes.contains(cell.getAttribute())) {
                        resultCells.add(cellNormalizer.normalize(cell.getValue().toString()));
                    }
                }
            }
        }
    }

    public static void computeExpectedCells(List<Tuple> expected, Set<String> expectedAttributes, Set<String> expectedCells, CellNormalizer cellNormalizer) {
        for (Tuple tuple : expected) {
            for (Cell cell : tuple.getCells()) {
                if (!cell.isOID()) {
                    expectedAttributes.add(cell.getAttribute());
                    expectedCells.add(cellNormalizer.normalize(cell.getValue().toString()));
                }
            }
        }
    }

}
