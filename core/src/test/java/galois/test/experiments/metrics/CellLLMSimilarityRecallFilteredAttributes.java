package galois.test.experiments.metrics;

import static galois.test.experiments.metrics.CellLLMSimilarityPrecisionFilteredAttributes.computeExpectedCellsWithAttrs;
import static galois.test.experiments.metrics.CellLLMSimilarityPrecisionFilteredAttributes.computeResultCellsWithAttrs;
import static galois.test.experiments.metrics.CellLLMSimilarityPrecisionFilteredAttributes.partitionByAttr;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CellLLMSimilarityRecallFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "CellLLMSimilarityRecallFilteredAttributes";
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
        double totCells = expectedCells.size(); // exclude the OIDs

        LLMDistance llmDistance = new LLMDistance();

        Map<String, Set<String>> resultCellsPartitions = partitionByAttr(resultCells);
        Map<String, Set<String>> expectedCellsPartitions = partitionByAttr(expectedCells);

        for (String attribute : expectedCellsPartitions.keySet()) {
            log.debug("Attribute: " + attribute);
            Set<String> partitionResultCells = resultCellsPartitions.get(attribute);
            Set<String> partitionExpectedCells = expectedCellsPartitions.get(attribute);
            log.debug("Result Partition: " + partitionResultCells);
            log.debug("Expected Partition: " + partitionExpectedCells);
            for (String expectedCell : partitionExpectedCells) {
                for (String resultCell : partitionResultCells) {
                    if (llmDistance.areCellSimilar(expectedCell, resultCell, attribute + ", value=")) {
                        log.debug("Match: " + resultCell + " -vs- " + expectedCell);
                        count++;
                        break;
                    }
                }
            }
        }

        //System.out.println("Count: "+count);
        //System.out.println("Total Cells: "+totCells);
        log.debug("Count: " + count + " --- Tot Cells: " + totCells);
        return count / totCells;
    }

}
