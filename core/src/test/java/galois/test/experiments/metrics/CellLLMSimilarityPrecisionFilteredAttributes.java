package galois.test.experiments.metrics;

import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CellLLMSimilarityPrecisionFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "CellLLMSimilarityPrecisionFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if (expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = new HashSet<>();
        Set<String> expectedAttributes = new HashSet<>();
        computeExpectedCellsWithAttrs(expected, expectedAttributes, expectedCells, cellNormalizer);

        Set<String> resultCells = new HashSet<>();
        computeResultCellsWithAttrs(result, expectedAttributes, resultCells, cellNormalizer);

        double count = 0.0;
        double totCells = resultCells.size(); // exclude the OIDs

        LLMDistance llmDistance = new LLMDistance();

        for (String resultCell : resultCells) {
            for (String expectedCell : expectedCells) {
                if (llmDistance.areCellSimilar(expectedCell, resultCell)) {
                    count++;
                    break;
                }
            }
        }
        return count / totCells;
    }

    public static void computeExpectedCellsWithAttrs(List<Tuple> expected, Set<String> expectedAttributes, Set<String> expectedCells, CellNormalizer cellNormalizer) {
        for (Tuple tuple : expected) {
            for (Cell cell : tuple.getCells()) {
                if (!cell.isOID()) {
                    expectedAttributes.add(cell.getAttribute());
                    String text = "attribute=" + cell.getAttribute() + ", value=" + cellNormalizer.normalize(cell.getValue().toString());
                    //String text = "value=" + cellNormalizer.normalize(cell.getValue().toString());
                    expectedCells.add(text);
                }
            }
        }
    }
    
   public static void computeResultCellsWithAttrs(List<Tuple> result, Set<String> expectedAttributes, Set<String> resultCells, CellNormalizer cellNormalizer) {
        for (Tuple tuple : result) {
            if (tuple != null && tuple.getCells() != null) {
                for (Cell cell : tuple.getCells()) {
                    if (!cell.isOID() && expectedAttributes.contains(cell.getAttribute())) {
                        String text = "attribute=" + cell.getAttribute() + ", value=" + cellNormalizer.normalize(cell.getValue().toString());
                        //String text = "value=" + cellNormalizer.normalize(cell.getValue().toString());
                        resultCells.add(text);
                    }
                }
            }
        }
    }
}
