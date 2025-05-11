package galois.test.experiments.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.IValue;
import speedy.model.database.Key;
import speedy.model.database.Tuple;

@Slf4j
public class TupleCellSimilarityFilteredAttributes implements IMetric {

    private CellNormalizer normalizer = new CellNormalizer();
    private EditDistance editDist = new EditDistance();
    private LLMDistance llmDistance = new LLMDistance();

    @Override
    public String getName() {
        return "TupleCellSimilarityFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        if (expected.isEmpty() && result.isEmpty()) {
            return 1.0;
        }
        if (expected.isEmpty() || result.isEmpty()) {
            return 0.0;
        }
        List<String> expectedAttributes = getAttributeNames(expected.get(0));
        
        return computeScoreWithAttrWithMoreDistinctValues(expected, filterAttributes(result, expectedAttributes));

        /*Set<Key> keysInDB = new HashSet<>();
        keysInDB.addAll(database.getKeys());
        keysInDB.addAll(database.getPrimaryKeys());
        Key key = findMaxKey(keysInDB, result);
        if (key == null) {
            return computeScoreWithAttrWithMoreDistinctValues(expected, result);
        } else {
            return computeScoreWithKey(key, expected, result);
        } */
    }

    private Double computeScoreWithAttrWithMoreDistinctValues(List<Tuple> expected, List<Tuple> result) {
        //Tuple firstTuple = expected.get(0);
        
        String attributeKey = getAttributeLikelyBeKey(expected);
//        log.debug("Attribute Key: " + attributeKey);
        int denominator = expected.size();
        double numerator = 0;
        
        Map<String, List<Tuple>> expectedPartition = createPartitionByAttribute(expected, attributeKey);
        Map<String, List<Tuple>> resultPartition = createPartitionByAttribute(result, attributeKey);
        for (String value : expectedPartition.keySet()) {
            List<Tuple> tuplesExpected = expectedPartition.get(value);
            List<Tuple> tuplesActual = resultPartition.get(value);
            if (tuplesActual != null) {
                numerator += computeMatches(tuplesExpected, tuplesActual);   
            }
        }
        return numerator / denominator;
    }

    private String getAttributeLikelyBeKey(List<Tuple> expected) {
        Map<String, Set<String>> valuesForAttr = new HashMap<>();
        for (Tuple tuple : expected) {
            for (Cell cell : tuple.getCells()) {
                if (!cell.isOID()) {
                    String attribute = cell.getAttribute();
                    Set<String> values = valuesForAttr.getOrDefault(attribute, new HashSet<>());
                    if (values.isEmpty()) {
                        valuesForAttr.put(attribute, values);
                    }
                    values.add(normalizer.normalize(cell.getValue().getPrimitiveValue()));
                }
            }
        }
        String attributeKey = null;
        int maxSize = 0;
        for (String attribute : valuesForAttr.keySet()) {
            Set<String> distincValues = valuesForAttr.get(attribute);
            if (distincValues.size() > maxSize) {
                attributeKey = attribute;
                maxSize = distincValues.size();
            }
        }
        return attributeKey;
    }

    private List<String> getAttributeNames(Tuple firstTuple) {
        List<Cell> cells = firstTuple.getCells();
        List<String> attributeNames = new ArrayList<>();
        for (Cell cell : cells) {
            if (!cell.isOID()) {
                attributeNames.add(cell.getAttribute());
            }
        }
        return attributeNames;
    }

    private Double computeScoreWithKey(Key key, List<Tuple> expected, List<Tuple> result) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Map<String, List<Tuple>> createPartitionByAttribute(List<Tuple> tuples, String attributeKey) {
        Map<String, List<Tuple>> partitions = new HashMap<>();
        for (Tuple tuple : tuples) {
            IValue value = getValueForAttr(tuple, attributeKey);
            if (value != null) {
                String normalized = normalizer.normalize(value.getPrimitiveValue());
                List<Tuple> tuplesWithValue = partitions.getOrDefault(normalized, new ArrayList<>());
                if (tuplesWithValue.isEmpty()) partitions.put(normalized, tuplesWithValue);
                tuplesWithValue.add(tuple);
            }
        }
        return partitions;
    }
    
    private IValue getValueForAttr(Tuple tuple, String attribute) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equalsIgnoreCase(attribute)) return cell.getValue();
        }
        return null;
    }
    
    private int computeMatches(List<Tuple> expected, List<Tuple> actual) {
        int counter = 0;
        List<Tuple> te = new ArrayList<>(expected);
        List<Tuple> ta = new ArrayList<>(actual);
        Iterator<Tuple> iteratorExpected = te.iterator();
        Iterator<Tuple> iteratorActual = ta.iterator();
        while (iteratorExpected.hasNext()) {
            Tuple tupleExpected = iteratorExpected.next();
            while (iteratorActual.hasNext()) {
                Tuple tupleActual = iteratorActual.next();
                if (match(tupleActual, tupleExpected)) {
                    counter++;
                    iteratorActual.remove();
                    if (tupleExpected != null) {
                        iteratorExpected.remove();
                    }
                    break;
                }
            }

        }
        return counter;
    }

    private boolean match(Tuple tupleActual, Tuple tupleExpected) {
//        log.debug("Match: " + tupleActual + " vs " + tupleExpected);
        if (tupleActual == null || tupleExpected == null) return false;
        for (Cell cell : tupleActual.getCells()) {
            if (!cell.isOID()) {
                Object actualValue = cell.getValue().getPrimitiveValue();
                IValue expectedValue = getValueForAttr(tupleExpected, cell.getAttribute());
                String actualNormalized = normalizer.normalize(actualValue);
                String expectedNormalized = normalizer.normalize(expectedValue.getPrimitiveValue());
                double threshold = expectedNormalized.length() * 0.1;
                if (editDist.getScoreForCells(expectedNormalized, actualNormalized, threshold) == false) {
                    String attributeName = cell.getAttribute()+": ";
                    return llmDistance.areCellSimilar(attributeName +expectedNormalized, attributeName+ actualNormalized, null);
//                    log.debug("Return false because: " + actualNormalized + " --- " + expectedNormalized);
//                    return false;
                }
            }
        }
//        log.debug("Return True");
        return true;
    }

    private List<Tuple> filterAttributes(List<Tuple> resultOriginal, List<String> expectedAttributes) {
        List<Tuple> result = new ArrayList<>();
        for (Tuple tuple : resultOriginal) {
            if (tuple != null) {
                Tuple filtered = filter(tuple, expectedAttributes);
                result.add(filtered);
            }
        }
        return result;
    }

    private Tuple filter(Tuple tuple, List<String> expectedAttributes) {
        Set<String> attributeSet = new HashSet<>(expectedAttributes);
        Tuple filtered = new Tuple(tuple.getOid());
        for (Cell cell : tuple.getCells()) {
            if (attributeSet.contains(cell.getAttribute())) {
                filtered.addCell(cell);
            }
        }
        return filtered;
    }

    private List<Tuple> removeNullTuples(List<Tuple> l) {
        List<Tuple> toReturn = new ArrayList<>();
        for (Tuple tuple : l) {
            if (tuple != null && !tuple.getCells().isEmpty()) {
                toReturn.add(tuple);
            }
        }
        return toReturn;
    }
}
