package galois.test.experiments.metrics;

import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

public class CellRecall implements IMetric {

    @Override
    public String getName() {
        return "CellRecall";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = expected.stream()
                .flatMap(tuple -> tuple.getCells().stream())
                .filter(cell -> !cell.isOID())
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .collect(Collectors.toSet());

        Set<String> resultCells = result.stream().flatMap(tuple -> tuple.getCells().stream())
                .filter(cell -> !cell.isOID())
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .collect(Collectors.toSet());

        double count = 0.0;
        double totCells = expectedCells.size(); // exclude the OIDs

        for (String expectedCell : expectedCells) {

            if(resultCells.stream().anyMatch(resultCell -> checkCell(expectedCell, resultCell))){
                count++;
                //System.out.println("Found: "+expectedCell);
            }

        }
        //System.out.println("Count: "+count);
        //System.out.println("Total Cells: "+totCells);

        return count / totCells;
    }


    private boolean checkCell(String expected, String result){

        // Regular expression to check if string starts with a letter
        // String startsWithLetter = "^[a-zA-Z].*";
        // Regular expression to check if string contains only numbers
        String containsOnlyNumbers = "^[0-9]+$";

        // Check if both strings start with a letter or a number
        /*if ((expected.matches(containsOnlyNumbers) && result.matches(containsOnlyNumbers))) {
            double expectedValue = Double.parseDouble(expected);
            double resultValue = Double.parseDouble(result);

            return expectedValue >= (resultValue - resultValue * (0.1)) && expectedValue <= (resultValue + resultValue * (0.1));
        }*/

        return expected.equals(result);
    }

}
