package galois.test.experiments.metrics;

// This class provides a way to calculate the Edit Distance metric
public class EditDistance{

    // Calculates the score for the given expected and result cells and threshold
    public boolean getScoreForCells(String expected, String result, double threshold) {

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
    public int editDistance(String str1, String str2)
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