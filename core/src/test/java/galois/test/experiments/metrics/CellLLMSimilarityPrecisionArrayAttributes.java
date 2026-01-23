package galois.test.experiments.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CellLLMSimilarityPrecisionArrayAttributes implements IMetric {

    // Regex to extract content inside quotes: "value"
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("\"([^\"]*)\"");

    @Override
    public String getName() {
        return "CellLLMSimilarityPrecisionArrayAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        if (expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }

        CellNormalizer cellNormalizer = new CellNormalizer();
        LLMDistance llmDistance = new LLMDistance();

        // 1. Index Expected Tuples by OID for fast lookup
        //    And collect all valid attributes from expected data to filter the results
        Map<String, Tuple> expectedMap = new HashMap<>();
        Set<String> expectedAttributes = new HashSet<>();
        
        for (Tuple t : expected) {
            String oid = getOID(t);
            expectedMap.put(oid, t);
            for (Cell c : t.getCells()) {
                if (!c.isOID()) {
                    expectedAttributes.add(c.getAttribute());
                }
            }
        }

        double globalMatches = 0.0;
        double globalTotCells = 0.0;
        double LLMCalls = 0.0;

        // 2. Iterate over Result Tuples
        for (Tuple resultTuple : result) {
            String oid = getOID(resultTuple);
            
            // Unfold the cells for this specific result tuple
            // We only care about attributes that exist in the expected schema
            Set<String> resultTupleCells = new HashSet<>();
            extractUnfoldedCells(resultTuple, expectedAttributes, resultTupleCells, cellNormalizer);
            
            // Add to the total count (Precision Denominator)
            globalTotCells += resultTupleCells.size();
            
            // Find the corresponding Expected Tuple
            Tuple expectedTuple = expectedMap.get(oid);
           
            if (expectedTuple != null) {
                // Unfold the cells for the corresponding expected tuple
                Set<String> expectedTupleCells = new HashSet<>();
                extractUnfoldedCells(expectedTuple, expectedAttributes, expectedTupleCells, cellNormalizer);
                MatchResult matchesResult = countMatchesForTuple(resultTupleCells, expectedTupleCells, llmDistance);
                globalMatches += matchesResult.matches;
                LLMCalls += matchesResult.LLMCalls;

                //globalMatches += countMatchesForTuple(resultTupleCells, expectedTupleCells, llmDistance);
            } else {
                log.debug("OID " + oid + " in result but not in expected. 0 matches for this tuple.");
            }
        }

        log.debug("Total Matches: " + globalMatches + " --- Total Result Cells: " + globalTotCells + " --- Total LLM calls: " + LLMCalls);
        
        if (globalTotCells == 0) return 0.0;
        return globalMatches / globalTotCells;
    }

    private MatchResult countMatchesForTuple(Set<String> resultCells, Set<String> expectedCells, LLMDistance llmDistance) {
        double matches = 0;
        
        Map<String, Set<String>> resultPartitions = partitionByAttr(resultCells);
        Map<String, Set<String>> expectedPartitions = partitionByAttr(expectedCells);

        double LLMCalls = 0;

        for (String attribute : resultPartitions.keySet()) {
            Set<String> rCells = resultPartitions.get(attribute);
            Set<String> eCells = expectedPartitions.get(attribute);

            if (eCells == null) continue; // Attribute exists in result tuple but not in expected tuple for this OID

            for (String eCell: eCells){
                String matchFound = "";
                for (String rCell: rCells){
                    if (eCell.equals(rCell)){
                        matchFound = rCell;
                        //log.debug("Match found: " + rCell + " -> " + matchFound);
                        matches++;
                    }
                }
                rCells.remove(matchFound);
            }

            for (String rCell : rCells) {
                //String matchFound = "";
                // for (String eCell : eCells) {
                //     // Check similarity restricted to this attribute
                //     if (eCell.equals(rCell)){
                //         matches++;
                //         log.debug("Match found: " + rCell + " -vs- " + eCell);
                //         matchFound = eCell;
                //         calls++;
                //         break;
                //     }
                //     LLMCalls++;
                //     calls++;
                //     if (llmDistance.areCellSimilar(eCell, rCell, attribute + ", value=")) {
                //         log.debug("Match found: " + rCell + " -vs- " + eCell);
                //         matches++;
                //         matchFound = eCell;
                //         break; // Move to next result cell after finding a match
                //     }
                // }
                // if (!matchFound.equals("")){
                //     eCells.remove(matchFound);
                // }
                String match = llmDistance.findSimilar(attribute, rCell, eCells);
                if (match != null && !match.isEmpty()) {
                    log.debug("Match found: " + rCell + " -> " + match);
                    matches++;
                    LLMCalls++;
                }
                else{
                    log.debug("Match NOT found: " + rCell + " -> " + eCells);
                }   

            }
        }
        return new MatchResult(matches, LLMCalls);
    }

    private void extractUnfoldedCells(Tuple tuple, Set<String> validAttributes, Set<String> targetSet, CellNormalizer cellNormalizer) {
        if (tuple == null || tuple.getCells() == null) return;

        for (Cell cell : tuple.getCells()) {
            // Skip OIDs and attributes not present in the Expected schema
            if (!cell.isOID() && validAttributes.contains(cell.getAttribute())) {
                unfoldAndAddCell(cell, targetSet, cellNormalizer);
            }
        }
    }

    private void unfoldAndAddCell(Cell cell, Set<String> targetSet, CellNormalizer cellNormalizer) {
        String rawValue = cell.getValue().toString().trim();
        String attribute = cell.getAttribute();

        // Check if value looks like a JSON list: ["...", "..."]
        if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
            Matcher matcher = LIST_ITEM_PATTERN.matcher(rawValue);
            while (matcher.find()) {
                // Group 1 is the content inside the quotes
                String singleItem = matcher.group(1);
                String normalizedValue = cellNormalizer.normalize(singleItem);
                String text = "attribute=" + attribute + ", value=" + normalizedValue;
                targetSet.add(text);
            }
        } else {
            // Standard behavior for non-list cells
            String normalizedValue = cellNormalizer.normalize(rawValue);
            String text = "attribute=" + attribute + ", value=" + normalizedValue;
            targetSet.add(text);
        }
    }

    private String getOID(Tuple tuple) {
        if (tuple.getOid() != null && tuple.getOid().getValue() != null) {
            return tuple.getOid().getValue().toString();
        }
        return "UNKNOWN";
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

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MatchResult {
        private double matches;
        private double LLMCalls;
    }
}