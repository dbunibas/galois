package galois.test.experiments.metrics;

import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.*;

@Slf4j
public class TupleCellSimilarityFilteredAttributes implements IMetric {

    private CellNormalizer normalizer = new CellNormalizer();
    private EditDistance editDist = new EditDistance();
    private LLMDistance llmDistance = new LLMDistance();
    private double thresholdEditDistance = 0.1;

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
//        List<String> expectedAttributes = Arrays.asList("surname");
//        List<String> expectedAttributes = Arrays.asList("surname", "jersey_number_euro2016");
        return computeScoreNoPartition(expected, filterAttributes(result, expectedAttributes));
        //return computeScoreWithAttrWithMoreDistinctValues(expected, filterAttributes(result, expectedAttributes));

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
    
    private Double computeScoreNoPartition(List<Tuple> expected, List<Tuple> result) {
        if (expected.size() > result.size()) return computeScoreNoPartition(result, expected); 
        double tp = 0;
        Set<Tuple> matchedExpected = new HashSet<>();
        Set<Tuple> matchedActual = new HashSet<>();
        for (Tuple tupleActual : result) {
            for (Tuple tupleExpected : expected) {
                if (matchedExpected.contains(tupleExpected)) continue;
                if (matchExact(tupleActual, tupleExpected)) {
                    tp++;
                    matchedExpected.add(tupleExpected);
                    matchedActual.add(tupleActual);
                    break;
                }
            }
            if (matchedActual.contains(tupleActual)) continue;
            for (Tuple tupleExpected : expected) {
                if (matchedExpected.contains(tupleExpected)) continue;
                if (matchSimilar(tupleActual, tupleExpected)) {
                    tp++;
                    matchedExpected.add(tupleExpected);
                    matchedActual.add(tupleActual);
                    break;
                }
            }
        }
        for (Tuple tupleActual : result) {
            if (matchedActual.contains(tupleActual)) continue;
            for (Tuple tupleExpected : expected) {
                if (matchedExpected.contains(tupleExpected)) continue;
                if (matchSimilarLLM(tupleActual, tupleExpected)) {
                    tp++;
                    matchedExpected.add(tupleExpected);
                    matchedActual.add(tupleActual);
                    break;
                }
            }
        }
        double fp = result.size() - tp;
        double fn = expected.size() - tp;
        double precision = tp / (tp + fp);
        double recall = tp / (tp + fn);
        if ((tp + fp)== 0) precision = 0;
        if ((tp + fn) == 0) recall = 0;
        if ((precision + recall) == 0) return 0.0;
        return (2 * precision * recall) / (precision + recall);
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
   
    private boolean matchExact(Tuple tupleActual, Tuple tupleExpected) {
        if (tupleActual == null || tupleExpected == null) return false;
        if (tupleActual.toStringNoOID().equalsIgnoreCase(tupleExpected.toStringNoOID())) {
//            log.error("Actual: " + tupleActual.toStringNoOID());
//            log.error("Expected: " + tupleExpected.toStringNoOID());
//            log.error("------------------------------");
//            log.info("MATCH exact");
            return true;
        }
        return false;
    }

    private boolean matchSimilar(Tuple tupleActual, Tuple tupleExpected) {
        if (tupleActual == null || tupleExpected == null) return false;
        List<String> categoricalAttributes = new ArrayList<>();
        List<String> numericalAttributes = new ArrayList<>();
        findTypes(tupleActual, categoricalAttributes, numericalAttributes);
//        log.error("Numerical Attributes: " + numericalAttributes);
//        log.error("Categorical Attributes: " + categoricalAttributes);
        if (!checkNumericalAttributes(tupleActual, tupleExpected, numericalAttributes)) {
//            log.error("NON MATCH due to numerical values");
            return false;
        }
        String signatureActual = generateSignatureNormalizeCategorical(tupleActual, categoricalAttributes);
        String signatureExpected = generateSignatureNormalizeCategorical(tupleExpected, categoricalAttributes);
        if (editDist.getScoreForCells(signatureExpected, signatureActual, thresholdEditDistance)) {
//            log.error("MATCH SIGNATURE and CHECK NUMERICAL");
            return true;
        }
        return false;
    }

    private boolean matchSimilarLLM(Tuple tupleActual, Tuple tupleExpected) {
        if(true){
            //TODO: matchSimilarLLM is currently disabled
            return false;
        }
        if (tupleActual == null || tupleExpected == null) {
            return false;
        }
        List<String> categoricalAttributes = new ArrayList<>();
        List<String> numericalAttributes = new ArrayList<>();
        findTypes(tupleActual, categoricalAttributes, numericalAttributes);
//        log.error("Numerical Attributes: " + numericalAttributes);
//        log.error("Categorical Attributes: " + categoricalAttributes);
        if (!checkNumericalAttributes(tupleActual, tupleExpected, numericalAttributes)) {
//            log.error("NON MATCH due to numerical values");
            return false;
        }
//        log.error("Starting Check using LLM Similarity");
        for (Cell cell : tupleActual.getCells()) {
            if (!cell.isOID()) {
                Object actualValue = cell.getValue().getPrimitiveValue();
                IValue expectedValue = getValueForAttr(tupleExpected, cell.getAttribute());
                String actualNormalized = normalizer.normalize(actualValue);
                String expectedNormalized = normalizer.normalize(expectedValue.getPrimitiveValue());
                if (actualNormalized.equalsIgnoreCase("null") || expectedNormalized.equalsIgnoreCase("null")) return false;
                String attributeName = cell.getAttribute() + ": ";
                if (!llmDistance.areCellSimilar(attributeName + expectedNormalized, attributeName + actualNormalized, attributeName)) {
                    return false;
                }
            }
        }
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

    private List<Tuple> findPossibleMatchesWithDistance(String value, String attribute, Map<String, List<Tuple>> resultPartition) {
        Set<String> candidateValues = resultPartition.keySet();
        String similarKeyValue = llmDistance.findSimilar(attribute, value, candidateValues);
        if (similarKeyValue == null || similarKeyValue.isEmpty()) return null;
        return resultPartition.get(similarKeyValue);
    }

    private void findTypes(Tuple tuple, List<String> categoricalAttributes, List<String> numericalAttributes) {
        for (Cell cell : tuple.getCells()) {
            if (cell.isOID()) continue;
            if (llmDistance.getNumber(cell.getValue().getPrimitiveValue().toString()) == null) {
                categoricalAttributes.add(cell.getAttribute());
            } else {
                numericalAttributes.add(cell.getAttribute());
            }
        }
    }

    private String generateSignatureNormalizeCategorical(Tuple tuple, List<String> categoricalAttributes) {
        StringBuilder signature = new StringBuilder();
        for (String categoricalAttribute : categoricalAttributes) {
            signature.append(categoricalAttribute).append("= ").append(normalizer.normalize(getValueForAttr(tuple, categoricalAttribute).getPrimitiveValue().toString())).append(", ");
        }
        if(signature.toString().trim().isEmpty()){
            return "";
        }
        signature = new StringBuilder(signature.toString().trim());
        return signature.substring(0, signature.length() - 1);
    }

    private boolean checkNumericalAttributes(Tuple actual, Tuple expected, List<String> numericalAttributes) {
        for (String numericalAttribute : numericalAttributes) {
            IValue actualValue = getValueForAttr(actual, numericalAttribute);
            IValue expectedValue = getValueForAttr(expected, numericalAttribute);
            if(actualValue == null && expectedValue == null){
                return true;
            }
            if(actualValue == null || expectedValue == null){
                return false;
            }
            log.trace("Compare: {} -- {}", actualValue, expectedValue);
            if (!llmDistance.areCellSimilar(expectedValue.getPrimitiveValue().toString(), actualValue.getPrimitiveValue().toString(), "")) return false;
        }
        return true;
    }

    private List<Tuple> getNonMatched(List<Tuple> actual, Set<Tuple> matched) {
        List<Tuple> nonMatched = new ArrayList<>();
        for (Tuple tuple : actual) {
            if (!matched.contains(tuple)) nonMatched.add(tuple);
        }
        return nonMatched;
    }

}
