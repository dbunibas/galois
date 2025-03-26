package galois.test.experiments.metrics;

import static galois.test.experiments.metrics.CellPrecisionFilteredAttributes.computeExpectedCells;
import static galois.test.experiments.metrics.CellPrecisionFilteredAttributes.computeResultCells;
import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CellSimilarityRecallFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "CellSimilarityRecallFilteredAttributes";
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
