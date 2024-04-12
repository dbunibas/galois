package galois.test.experiments.metrics;

import speedy.model.database.Cell;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

// This class implements the IMetric interface and provides a way to calculate the Edit Distance metric
public class EditDistance implements IMetric{

    // Returns the name of the metric
    @Override
    public String getName() {
        return "EditDistance";
    }

    // Calculates the score of the Edit Distance metric for the given database and expected and result tuples
    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        // Initialize count and totalTuple variables
        double count = 0.0;
        double totalTuple = expected.size();

        // Create a HashSet to keep track of visited tuples
        Set<Integer> visited = new HashSet<>();

        // Iterate over each expected tuple
        for (Tuple expectedTuple : expected) {
            // Iterate over each result tuple
            for (int j = 0; j < result.size(); j++) {
                Tuple resultTuple = result.get(j);

                // If the result tuple has already been visited, skip it
                if (visited.contains(j)) continue;

                // If the score for the expected and result tuples is true, increment the count and add the index to the visited set
                if (getScoreForTuples(expectedTuple, resultTuple)) {
                    System.out.println("Expected: " + expectedTuple);
                    System.out.println("Result: " + resultTuple);
                    count++;
                    visited.add(j);
                    break;
                }
            }
        }

        // Return the score, which is the count divided by the total number of tuples
        return count / totalTuple;
    }

    // Calculates the score for the given expected and result tuples
    private boolean getScoreForTuples(Tuple expected, Tuple result) {

        // Get the cells from the expected and result tuples
        List<Cell> expectedCells = expected.getCells();
        List<Cell> resultCells = result.getCells();
        int count = 0;
        double threshold = 0.0;
        CellNormalizer cellNormalizer = new CellNormalizer();

        // Iterate over each cell in the expected tuple
        for (int i = 0; i < expectedCells.size(); i++) {
            // If the cell is an OID, skip it
            if (expectedCells.get(i).isOID()) continue;

            // Normalize the cell values
            String expectedCell = cellNormalizer.normalize(expectedCells.get(i).getValue().toString());
            String resultCell = cellNormalizer.normalize(resultCells.get(i).getValue().toString());

            // Calculate the threshold
            threshold = expectedCells.get(i).getValue().toString().length() * 0.1;

            // If the score for the cells is true, increment the count
            if (getScoreForCells(expectedCell, resultCell, threshold)) {
                count++;
            }

        }

        // Return true if the count is equal to the size of the expected cells minus 1, otherwise return false
        return count == (expectedCells.size() - 1);

    }

    // Calculates the score for the given expected and result cells and threshold
    private boolean getScoreForCells(String expected, String result, double threshold) {

        // Regular expression to check if string contains only numbers
        String containsOnlyNumbers = "^[0-9]+$";

        // If both the expected and result cells start with a number, calculate the score based on their values
        if ((expected.matches(containsOnlyNumbers) && result.matches(containsOnlyNumbers))) {
            double expectedValue = Double.parseDouble(expected);
            double resultValue = Double.parseDouble(result);

            // Return true if the expected value is within 10% of the result value, otherwise return false
            return expectedValue >= (resultValue - resultValue * (0.1)) && expectedValue <= (resultValue + resultValue * (0.1));
        }

        // Calculate the edit distance between the expected and result cells
        double distance = editDistance(expected, result);
        // Return true if the distance is less than or equal to the threshold, otherwise return false
        return distance <= threshold;
    }

    // Calculates the edit distance between the given strings
    private int editDistance(String str1, String str2)
    {
        // Get the lengths of the two input strings.
        int m = str1.length();
        int n = str2.length();

        // Initialize an array to store the current row of edit distances.
        int[] curr = new int[n + 1];

        // Initialize the first row with values 0 to n.
        for (int j = 0; j <= n; j++) {
            curr[j] = j;
        }

        int previous;
        for (int i = 1; i <= m; i++) {
            // Store the value of the previous row's first column.
            previous = curr[0];
            curr[0] = i;

            for (int j = 1; j <= n; j++) {
                // Store the current value before updating it.
                int temp = curr[j];

                // Check if the characters at the current positions in str1 and str2 are the same.
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    // If they are the same, no additional cost is incurred.
                    curr[j] = previous;
                }
                else {
                    // If the characters are different, calculate the minimum of three operations: deletion, insertion, and substitution.
                    curr[j] = 1 + Math.min(Math.min(previous, curr[j - 1]), curr[j]);
                }
                // Update the previous value to the stored temporary value.
                previous = temp;
            }
        }
        // The value in the last cell of the current row represents the edit distance.
        return curr[n];
    }
}