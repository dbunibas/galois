package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.Key;

@Slf4j
public class TupleLLMSimilarityConstraintFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "TupleLLMSimilarityConstraintFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        if (database != null && database.getKeys().isEmpty() && database.getPrimaryKeys().isEmpty()) {
            return computeWithoutER(expected, result);
        } else {
            return computeWithER(database, expected, result);
        }
    }

    private Double computeWithoutER(List<Tuple> expected, List<Tuple> result) {
        LLMDistance distance = new LLMDistance();
        try {
            if (expected.isEmpty() && result.isEmpty()) {
                return 1.0;
            }
            List<Tuple> expectedSorted = expected.stream()
                    .sorted(Comparator.comparing(tuple -> tuple.getCells().get(1).getValue().toString()))
                    .toList();
            String attributeToSort = expectedSorted.getFirst().getCells().get(1).getAttribute();
//            log.info("Attribute Sort: " + attributeToSort);
            expectedSorted = new ArrayList<>(expectedSorted);
            int countExpected = expectedSorted.size();
            Set<String> filteredAttributes = new HashSet<>();
            for (Tuple tuple : expectedSorted) {
                for (Cell cell : tuple.getCells()) {
                    if (!cell.isOID()) {
                        filteredAttributes.add(cell.getAttribute());
                    }
                }
            }
            List<Tuple> resultWithAttributes = new ArrayList<>();
            for (Tuple tupleResult : result) {
                Tuple tupleWithAttrs = getTuple(tupleResult, filteredAttributes);
                resultWithAttributes.add(tupleWithAttrs);

            }
            List<Tuple> resultWithAttributesSorted = resultWithAttributes.stream()
                    .sorted(Comparator.comparing(tuple -> getStringValueForAttr(tuple, attributeToSort)))
                    .toList();
            resultWithAttributes = new ArrayList<>(resultWithAttributesSorted);
            int count = 0;
            Iterator<Tuple> iteratorResult = resultWithAttributes.iterator();

//            log.info("Actual:\n" + resultWithAttributes);
//            log.info("Expected:\n" + expectedSorted);
            while (iteratorResult.hasNext()) {
                Tuple actualTuple = iteratorResult.next();
                String actualTupleString = actualTuple.toStringNoOID();
//                log.info("Actual: " + actualTupleString);
                Iterator<Tuple> iteratorExpected = new ArrayList<>(expectedSorted).iterator();
                while (iteratorExpected.hasNext()) {
                    Tuple expectedTuple = iteratorExpected.next();
                    String expectedTupleToString = expectedTuple.toStringNoOID();
//                    log.info("Expected: " + expectedTupleToString);
//                    log.info(actualTupleString + " * vs * " + expectedTupleToString);
                    if (distance.areTupleSimilar(actualTupleString, expectedTupleToString)) {
//                        log.info("Similar");
                        count++;
                        iteratorResult.remove();
                        iteratorExpected.remove();
                        break;
                    }
                }
            }
            return ((double) count) / countExpected;
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return -1.0;
        }
    }

    private Tuple getTuple(Tuple tupleResult, Set<String> filteredAttributes) {
        Tuple tupleReturn = new Tuple(tupleResult.getOid());
        for (Cell cell : tupleResult.getCells()) {
            if (filteredAttributes.contains(cell.getAttribute())) {
                tupleReturn.addCell(cell);
            }
        }
        return tupleReturn;
    }

    public String getStringValueForAttr(Tuple tuple, String attributeName) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equalsIgnoreCase(attributeName)) {
                return cell.getValue().getPrimitiveValue().toString();
            }
        }
        return null;
    }

    private Double computeWithER(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        Set<Key> keysInDB = new HashSet<>();
        keysInDB.addAll(database.getKeys());
        keysInDB.addAll(database.getPrimaryKeys());
        if (expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }
        Key key = findMaxKey(keysInDB, result);
        if (key == null) {
            return computeWithoutER(expected, result);
        }
        log.debug("Key: " + key);
        LLMDistance distance = new LLMDistance();

        Map<String, List<Tuple>> resultBlocking = getBlocking(result, key);
        Map<String, List<Tuple>> expectedBlocking = getBlocking(expected, key);
        log.debug("resultBlocking: " + resultBlocking.size());
        log.debug("expectedBlocking: " + expectedBlocking.size());
        int denominatorPrecision = result.size();
        int denominatorRecall = expected.size();
        
        double numeratorPrecision = 0;
        double numeratorRecall = 0;
      
        for (String keyForResultBlock : resultBlocking.keySet()) {
            log.debug("keyForResultBlock: " + keyForResultBlock);
            List<Tuple> partitionResult = resultBlocking.get(keyForResultBlock);
            List<Tuple> partitionExpected = expectedBlocking.get(keyForResultBlock);
            log.debug("partitionResult: " + partitionResult);
            log.debug("partitionExpected: " + partitionExpected);
            for (Tuple tupleResult : partitionResult) {
                if (partitionExpected != null) {
                    for (Tuple tupleExpected : partitionExpected) {
                        if (distance.areTupleSimilar(tupleResult.toStringNoOID(), tupleExpected.toStringNoOID())) {
                            log.debug("Match: " + tupleResult + " -vs- " + tupleExpected);
                            numeratorPrecision++;
                            break;
                        }
                    }
                }
            }
        }
        for (String keyForExpectedBlock : expectedBlocking.keySet()) {
            log.debug("keyForExpectedBlock: " + keyForExpectedBlock);
            List<Tuple> partitionResult = resultBlocking.get(keyForExpectedBlock);
            List<Tuple> partitionExpected = expectedBlocking.get(keyForExpectedBlock);
            log.debug("partitionResult: " + partitionResult);
            log.debug("partitionExpected: " + partitionExpected);
            for (Tuple tupleExpected : partitionExpected) {
                if (partitionResult != null) {
                    for (Tuple tupleResult : partitionResult) {
                        if (distance.areTupleSimilar(tupleResult.toStringNoOID(), tupleExpected.toStringNoOID())) {
                           log.debug("Match: " + tupleResult + " -vs- " + tupleExpected);
                            numeratorRecall++;
                            break;
                        }
                    }
                }
            } 
        }
        double precision = numeratorPrecision / denominatorPrecision;
        double recall = numeratorRecall / denominatorRecall;
        if(precision + recall == 0) return 0.0;
        return 2 * (precision * recall) / (precision + recall);
    }

    private Map<String, List<Tuple>> getBlocking(List<Tuple> tuples, Key key) {
        Map<String, List<Tuple>> blocking = new HashMap<>();
        for (Tuple tuple : tuples) {
            String keyString = getBlockKey(tuple, key);
            List<Tuple> tuplesForBlock = blocking.getOrDefault(keyString, new ArrayList<>());
            if (tuplesForBlock.isEmpty()) blocking.put(keyString, tuplesForBlock);
            tuplesForBlock.add(tuple);
        }
        return blocking;
    }

    public static Key findMaxKey(Set<Key> keysInDB, List<Tuple> result) {
        int maxCoverage = 0;
        Key keyMax = null;
        for (Key key : keysInDB) {
            if (match(key, result.get(0))) {
                if (key.getAttributes().size() > maxCoverage) {
                    maxCoverage = key.getAttributes().size();
                    keyMax = key;
                }
            }
        }
        return keyMax;
    }

    private static boolean match(Key key, Tuple tuple) {
        for (AttributeRef attribute : key.getAttributes()) {
            if (tuple.getCell(attribute) == null) {
                return false;
            }
        }
        return true;
    }

    private String getBlockKey(Tuple tuple, Key key) {
        String keyString = "";
        for (AttributeRef attribute : key.getAttributes()) {
//            keyString += attribute.getName() + ": " + tuple.getCell(attribute) + ";";
            keyString += attribute.getName() + ": " + getCellFromAttr(tuple, attribute).getValue().getPrimitiveValue() + ";";
        }
        return keyString.toLowerCase();
    }
    
    private Cell getCellFromAttr(Tuple tuple, AttributeRef aRef) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equalsIgnoreCase(aRef.getName())) {
                return cell;
            }
        }
        return null;
    }
}
