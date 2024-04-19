package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.*;

import speedy.model.database.Cell;

public class TupleConstraint implements  IMetric{
    @Override
    public String getName() {
        return "TupleConstraint";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellNormalizer cellNormalizer = new CellNormalizer();

        List<Tuple> expectedSorted = expected.stream()
                .sorted(Comparator.comparing(tuple -> tuple.getCells().get(1).getValue().toString()))
                .toList();

        List<List<String>> expectedCells = expectedSorted.stream().map(tuple -> tuple.getCells().stream()
                        .filter(cell -> !cell.isOID())
                        .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                        .toList())
                .toList();

        int numberOfAttributes;
        if(result.get(1).getCells().size() != expected.get(1).getCells().size()){
            numberOfAttributes = result.get(1).getCells().size() -1;
            //remove from expectedCells the i-th element greater than number of attributes
            expectedCells = expectedCells.stream().map(list -> list.subList(0, numberOfAttributes)).toList();
        }

        List<Tuple> resultSorted = result.stream()
                .sorted(Comparator.comparing(tuple -> tuple.getCells().get(1).getValue().toString()))
                .toList();

        List<List<String>> resultCells = resultSorted.stream().map(tuple -> tuple.getCells().stream()
                        .filter(cell -> !cell.isOID())
                        .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                        .toList())
                .toList();

        HashMap<List<String>, Integer> expectedCellCount = new HashMap<>();
        HashMap<List<String>, Integer> resultCellCount = new HashMap<>();

        for (List<String> expectedCell : expectedCells) {
            expectedCellCount.put(expectedCell, expectedCellCount.getOrDefault(expectedCell, 0) + 1);
        }

        for (List<String> resultCell : resultCells) {
            resultCellCount.put(resultCell, resultCellCount.getOrDefault(resultCell, 0) + 1);
        }

        List<Boolean> cardinality = new ArrayList<>();


        for (Map.Entry<List<String>, Integer> entry : expectedCellCount.entrySet()) {
            if(Objects.equals(resultCellCount.get(entry.getKey()), entry.getValue())){
                cardinality.add(true);
            }
            else {
                cardinality.add(false);
            }
        }

        //return the sum of true values over cardinality over the length of cardinality
        return cardinality.stream().mapToDouble( value -> value ? 1 : 0).sum() / (double) cardinality.size();
    }
}
