package galois.test.experiments.metrics;

import java.util.HashMap;
import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        
        Map<String, Set<String>> resultCellsPartitions = partitionByAttr(resultCells);
        Map<String, Set<String>> expectedCellsPartitions = partitionByAttr(expectedCells);
        log.debug("resultCellsPartitions: " + resultCellsPartitions.size());
        log.debug("expectedCellsPartitions: " + expectedCellsPartitions.size());

        for (String attribute : resultCellsPartitions.keySet()) {
            log.debug("Attribute: " + attribute);
            Set<String> partitionResultCells = resultCellsPartitions.get(attribute);
            Set<String> partitionExpectedCells = expectedCellsPartitions.get(attribute);
            log.debug("Result Partition: " + partitionResultCells);
            log.debug("Expected Partition: " + partitionExpectedCells);
            for (String resultCell : partitionResultCells) {
                for (String expectedCell : partitionExpectedCells) {
                    if (llmDistance.areCellSimilar(expectedCell, resultCell, attribute + ", value=")) {
                        log.debug("Match: " + resultCell + " -vs- " + expectedCell );
                        count++;
                        break;
                    }
                }
            }
        }

//        for (String resultCell : resultCells) {
//            for (String expectedCell : expectedCells) {
//                if (llmDistance.areCellSimilar(expectedCell, resultCell)) {
//                    count++;
//                    break;
//                }
//            }
//        }
        
        log.debug("Count: " + count + " --- Tot Cells: " + totCells);
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

    public static Map<String, Set<String>> partitionByAttr(Set<String> cells) {
        Map<String, Set<String>> partitions = new HashMap<>();
        for (String cell : cells) {
            String attr = getAttribute(cell);
            Set<String> cellsForAttr = partitions.getOrDefault(attr, new HashSet<>());
            if (cellsForAttr.isEmpty()) partitions.put(attr, cellsForAttr);
            cellsForAttr.add(cell);
        }
        return partitions;
    }

    public static String getAttribute(String text) {
        StringTokenizer tokenizer = new StringTokenizer(text, ",");
        return tokenizer.nextToken().trim();
    }
}
