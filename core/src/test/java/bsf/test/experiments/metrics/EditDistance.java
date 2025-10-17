package bsf.test.experiments.metrics;

import org.apache.commons.text.similarity.LevenshteinDistance;
import queryexecutor.model.database.Tuple;

// This class provides a way to calculate the Edit Distance metric
public class EditDistance {
    
    private double thresholdForNumeric = 0.1;

    // Calculates the score for the given expected and result cells and threshold
    public boolean getScoreForCells(String expected, String result, double threshold) {

//        // Regular expression to check if string contains only numbers
//        String containsOnlyNumbers = "^[0-9]+$";
//
//        // If both the expected and result cells start with a number, calculate the score based on their values
//        if ((expected.matches(containsOnlyNumbers) && result.matches(containsOnlyNumbers))) {
//            double expectedValue = Double.parseDouble(expected);
//            double resultValue = Double.parseDouble(result);
//
//            // Return true if the expected value is within 10% of the result value, otherwise return false
//            return expectedValue >= (resultValue - resultValue * (0.1)) && expectedValue <= (resultValue + resultValue * (0.1));
//        }
        try {
            double expectedValue = Double.parseDouble(expected.replace(",", "."));
            double resultValue = Double.parseDouble(result.replace(",", "."));
            // Return true if the actual is within X% of the expected
            double variation = Math.abs((expectedValue - resultValue) / expectedValue);
            return variation <= thresholdForNumeric;
        } catch (NumberFormatException nfe) {
            // do nothing
        }
        // Calculate the edit distance between the expected and result cells
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        int distance = levenshteinDistance.apply(expected, result);
        int expectedLength = expected.length();
        double maxSize = Math.ceil(expectedLength * (1.0 + threshold));
        double minsize = Math.floor(expectedLength * (1.0 - threshold));
        return (minsize <= distance) && (distance <= maxSize);
        // Return true if the distance is less than or equal to the threshold, otherwise return false
//        return distance <= threshold;
    }
    
    public Double getScoreForTuple(Tuple expected, Tuple actual) {
       if (expected == null || actual == null) return null;
       String s1 = expected.toStringNoOID();
       String s2 = actual.toStringNoOID();
       LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
       double distance = levenshteinDistance.apply(s1, s2);
       return distance;
    }

}
