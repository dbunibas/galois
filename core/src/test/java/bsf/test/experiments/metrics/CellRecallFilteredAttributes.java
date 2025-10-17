package bsf.test.experiments.metrics;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class CellRecallFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "CellRecallFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if(expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = new HashSet<>();
        Set<String> expectedAttributes = new HashSet<>();
        CellPrecisionFilteredAttributes.computeExpectedCells(expected, expectedAttributes, expectedCells, cellNormalizer);
  

        Set<String> resultCells = new HashSet<>();
        CellPrecisionFilteredAttributes.computeResultCells(result, expectedAttributes, resultCells, cellNormalizer);

        // Calculate the intersection size directly
        Set<String> commonCells = new HashSet<>(expectedCells);
        commonCells.retainAll(resultCells);

        double count = commonCells.size(); // Number of common cells
        double totalExpectedCells = expectedCells.size(); // Exclude the OIDs

        return count / totalExpectedCells;
    }
}
