package bsf.test.experiments.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.Key;
import queryexecutor.model.database.Tuple;

@Slf4j
public class TupleCellSimilarityFilteredAttributes implements IMetric {

    private CellNormalizer normalizer = new CellNormalizer();
    private EditDistance editDist = new EditDistance();
    private LLMDistance llmDistance = new LLMDistance();
    private double thresholdEditDistance = 0.1;
    private int maxSorted = 10;

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
        List<Tuple> resultNotNull = this.removeNullTuples(result);
        log.debug("--- Get Score");
        log.debug("Result size: " + resultNotNull.size());
        log.debug("Expected size: " + expected.size());
        return computeScoreNoPartition(expected, filterAttributes(resultNotNull, expectedAttributes), expectedAttributes);
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
    
    private Double computeScoreNoPartition(List<Tuple> expected, List<Tuple> result, List<String> expectedAttributes) {
        //if (true) return 0.0;
        if (expected.size() < result.size()) return computeScoreNoPartition(result, expected, expectedAttributes); 
        double tp = 0;
        Map<Tuple,Tuple> matchedActualExpected = new HashMap<>();
        Set<Tuple> matchedExpected = new HashSet<>();
        Set<Tuple> matchedActual = new HashSet<>();
        log.info("Actual: " + result.size());
        log.info("Expected: " + expected.size());
        for (Tuple tupleActual : result) {
            if (tupleActual.getCells().isEmpty()) continue;
            for (Tuple tupleExpected : expected) {
                if (tupleExpected.getCells().isEmpty()) continue;
                if (matchedExpected.contains(tupleExpected)) continue;
                if (matchExact(tupleActual, tupleExpected)) {
                    tp++;
                    matchedExpected.add(tupleExpected);
                    matchedActual.add(tupleActual);
                    matchedActualExpected.put(tupleActual, tupleExpected);
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
                    matchedActualExpected.put(tupleActual, tupleExpected);
                    break;
                }
            }
        }
        List<Tuple> remainingActual = result.stream().filter(t -> !matchedActual.contains(t)).toList();
        List<Tuple> remainingExpected = expected.stream().filter(t -> !matchedExpected.contains(t)).toList();
        log.info("Tuples remained after fast checks:");
        log.info("Actual: " + remainingActual.size());
        log.info("Expected: " + remainingExpected.size());
//        float percentage = ((float)remainedActual) / remainedExpected;
        //if (percentage > 0.05) {
        boolean computeApprox = true;
        if (computeApprox) {
            log.info("Start LLM similarity");
            for (Tuple tupleActual : remainingActual) {
                if (tupleActual.getCells().isEmpty()) continue;
                if (matchedActual.contains(tupleActual)) {
                    continue;
                }
                log.debug("Actual Tuple: " + tupleActual.toStringNoOID());
                List<Tuple> expectedSorted = sortByTuple(tupleActual, remainingExpected, null);
                List<Tuple> expectedToCheck = expectedSorted;
                if (expectedToCheck.size() > maxSorted) {
                    expectedToCheck = expectedToCheck.subList(0, maxSorted);
                }
                // se expectedSorted > soglia (10 o 100)
                // 1) se minore soglia provo tutto con tutto Tupla per Tupla LLM as Judge
                // 2) altrimenti chiedo ad LLM se nelle restanti c'è una tupla simile, se si me la faccio restituire e poi faccio controllo Tupla LLM as Judge
                log.debug("Expected Check Sorted: \n" + expectedToCheck);
                for (Tuple tupleExpected : expectedToCheck) {
                    if (tupleExpected.getCells().isEmpty()) continue;
                    if (matchedExpected.contains(tupleExpected)) {
                        continue;
                    }
                    log.debug("\tExpected Tuple: " + tupleExpected.toStringNoOID());
                    if (matchSimilarLLM(tupleActual, tupleExpected, expectedAttributes)) {
                        log.debug("\tMatch");
                        tp++;
                        matchedExpected.add(tupleExpected);
                        matchedActual.add(tupleActual);
                        matchedActualExpected.put(tupleActual, tupleExpected);
                        break;
                    }
                }
                if (matchedActual.contains(tupleActual)) {
                    continue;
                }
                if (expectedToCheck.size() > maxSorted) {
                    Tuple possibleMatch = askLLMSimilarTuple(tupleActual, expectedToCheck.subList(10, expectedToCheck.size()), expectedAttributes);
                    if (possibleMatch == null) continue;
                     if (matchSimilarLLM(tupleActual, possibleMatch, expectedAttributes)) {
                        log.debug("\tMatch");
                        tp++;
                        matchedExpected.add(possibleMatch);
                        matchedActual.add(tupleActual);
                        matchedActualExpected.put(tupleActual, possibleMatch);
                        break;
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Tuple Expected: ");
            for (Tuple te : expected) {
                log.debug("TE: " + te);
            }
            log.debug("Tuple Actual: ");
            for (Tuple ta : result) {
                log.debug("TA: " + ta);
            }
            log.debug("Tuple Matched: ");
            for (Tuple ta : matchedActualExpected.keySet()) {
                Tuple te = matchedActualExpected.get(ta);
                log.debug("TA: " + ta);
                log.debug("TE: " + te);
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
        if (tupleActual.getCells().isEmpty() || tupleExpected.getCells().isEmpty()) return false;
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
        if (tupleActual.getCells().isEmpty() || tupleExpected.getCells().isEmpty()) return false;
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
            log.debug("MATCH SIGNATURE and CHECK NUMERICAL");
            return true;
        }
        return false;
    }

    private boolean matchSimilarLLM(Tuple tupleActual, Tuple tupleExpected, List<String> expectedAttributes) {
        if (tupleActual == null || tupleExpected == null) {
            return false;
        }
        List<String> categoricalAttributes = new ArrayList<>();
        List<String> numericalAttributes = new ArrayList<>();
        findTypes(tupleActual, categoricalAttributes, numericalAttributes);
//        log.error("Numerical Attributes: " + numericalAttributes);
//        log.error("Categorical Attributes: " + categoricalAttributes);
        if (!checkNumericalAttributes(tupleActual, tupleExpected, numericalAttributes)) {
            log.debug("NON MATCH due to numerical values");
            return false;
        }
//        log.error("Starting Check using LLM Similarity");
//        for (Cell cell : tupleActual.getCells()) {
//            if (!cell.isOID()) {
//                Object actualValue = cell.getValue().getPrimitiveValue();
//                IValue expectedValue = getValueForAttr(tupleExpected, cell.getAttribute());
//                String actualNormalized = normalizer.normalize(actualValue);
//                String expectedNormalized = normalizer.normalize(expectedValue.getPrimitiveValue());
//                if (actualNormalized.equalsIgnoreCase("null") || expectedNormalized.equalsIgnoreCase("null")) return false;
//                String attributeName = cell.getAttribute() + ": ";
//                if (!llmDistance.areCellSimilar(attributeName + expectedNormalized, attributeName + actualNormalized, attributeName)) {
//                    log.debug("NON MATCH due to: (" + attributeName +")" + expectedNormalized + " vs " + actualNormalized);
//                    return false;
//                }
//            }
//        }
        return llmDistance.areTupleSimilar(linearizeTuple1(tupleActual, expectedAttributes), linearizeTuple1(tupleExpected, expectedAttributes));
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

    private List<Tuple> findPossibleMatchesWithDistance(String value, String attribute,  Map<String, List<Tuple>> resultPartition) {
        Set<String> candidateValues = resultPartition.keySet();
        String similarKeyValue = llmDistance.findSimilar(attribute, value, candidateValues);
        if (similarKeyValue == null || similarKeyValue.isEmpty()) return null;
        return resultPartition.get(similarKeyValue);
    }

    private void findTypes(Tuple tuple, List<String> categoricalAttributes, List<String> numericalAttributes) {
        for (Cell cell : tuple.getCells()) {
            if (cell.isOID()) continue;
            IValue value = cell.getValue();
            String cellValue = "null";
            if (value != null) {
                cellValue = value.getPrimitiveValue().toString();
            }
            if (llmDistance.getNumber(cellValue) == null) {
                categoricalAttributes.add(cell.getAttribute());
            } else {
                numericalAttributes.add(cell.getAttribute());
            }
        }
    }
    
    private String generateSignatureNormalizeCategorical(Tuple tuple, List<String> categoricalAttributes) {
        if (categoricalAttributes == null || categoricalAttributes.isEmpty()) return "";
        String signature = "";
        for (String categoricalAttribute : categoricalAttributes) {
            IValue valueForAttr = getValueForAttr(tuple, categoricalAttribute);
            String value = "null";
            if (valueForAttr != null) {
                value = valueForAttr.getPrimitiveValue().toString();
            }
            signature += categoricalAttribute + "= "+ normalizer.normalize(value) + ", ";
        }
        signature = signature.trim();
        return signature.substring(0, signature.length() - 1);
    }
    
    private boolean checkNumericalAttributes(Tuple actual, Tuple expected, List<String> numericalAttributes) {
        for (String numericalAttribute : numericalAttributes) {
            IValue actualValue = getValueForAttr(actual, numericalAttribute);
            IValue expectedValue = getValueForAttr(expected, numericalAttribute);
            //log.error("Compare: " + actualValue + "---" + expectedValue);
            if (actualValue == null || expectedValue == null) return false;
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

    private List<Tuple> sortByTuple(Tuple tupleActual, List<Tuple> expected, Integer maxSize) {
        List<Tuple> sorted = new ArrayList<>();
        List<RankedTuple> ranked = new ArrayList<>();
        for (Tuple tupleExpected : expected) {
            Double scoreForTuple = editDist.getScoreForTuple(tupleExpected, tupleActual);
            if (scoreForTuple != null) ranked.add(new RankedTuple(tupleExpected, scoreForTuple));
        }
        Collections.sort(ranked);
        for (RankedTuple rankedTuple : ranked) {
            sorted.add(rankedTuple.tuple);
            if (maxSize != null && sorted.size() == maxSize) break;
        }
        return sorted;
    }

    private Tuple askLLMSimilarTuple(Tuple tupleActual, List<Tuple> expected, List<String> expectedAttributes) {
        String reference = linearizeTuple2(tupleActual, expectedAttributes);
        String listTuple = "";
        for(int i = 0; i < expected.size(); i++) {
            Tuple tuple = expected.get(i);
            String t = linearizeTupleWithPos(tuple, expectedAttributes, i);
            listTuple += t + "\n";
        }
        int pos = llmDistance.getSimilar(reference, listTuple);
        if (pos == -1) return null;
        return expected.get(pos);
    }
    
    private String linearizeTuple1(Tuple tuple, List<String> expectedAttributes) {
        Map<String, IValue> tupleMap = getTupleMap(tuple);
        String tupleString = "";
        for (String attributeName : expectedAttributes) {
            IValue ivalue = tupleMap.get(attributeName);
            tupleString += "- " + attributeName + ": " + ivalue.getPrimitiveValue().toString() + "\n";
        }
        return tupleString;
    }

    private Map<String, IValue> getTupleMap(Tuple tuple) {
        Map<String, IValue> tupleMap = new HashMap<>();
        for (Cell cell : tuple.getCells()) {
            tupleMap.put(cell.getAttribute(), cell.getValue());
        }
        return tupleMap;
    }
    
    private String linearizeTuple2(Tuple tuple, List<String> expectedAttributes) {
        Map<String, IValue> tupleMap = getTupleMap(tuple);
        String tupleString = "";
        for (String attributeName : expectedAttributes) {
            IValue ivalue = tupleMap.get(attributeName);
            tupleString += attributeName + ": " + ivalue.getPrimitiveValue().toString() + ";";
        }
        return tupleString;
    }
    
    private String linearizeTupleWithPos(Tuple tuple, List<String> expectedAttributes, int pos) {
        String tupleString = linearizeTuple2(tuple, expectedAttributes);
        tupleString = "Index: " + pos + " - " + tupleString;
        return tupleString;
    }
    
    private class RankedTuple implements Comparable<RankedTuple>{
        private Tuple tuple;
        private Double score;

        public RankedTuple(Tuple tuple, Double score) {
            this.tuple = tuple;
            this.score = score;
        }
        
        @Override
        public int compareTo(RankedTuple o) {
            return this.score.compareTo(o.score);
        }
    }

}
