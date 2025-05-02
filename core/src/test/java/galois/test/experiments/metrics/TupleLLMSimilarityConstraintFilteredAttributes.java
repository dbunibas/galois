package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Cell;

@Slf4j
public class TupleLLMSimilarityConstraintFilteredAttributes implements IMetric {

    @Override
    public String getName() {
        return "TupleLLMSimilarityConstraintFilteredAttributes";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
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
}
