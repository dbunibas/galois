package galois.test.experiments.metrics;

import java.util.*;
import java.util.stream.IntStream;

import speedy.model.database.IDatabase;
import speedy.model.database.Cell;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class SpearmanCorrelation implements IMetric {

    /**
     * Calculates Spearman Correlation for two lists of scores.
     * Aligns data by index (assuming index 0 in list1 corresponds to index 0 in list2).
     * Filters out NaN values to maintain alignment of "Common IDs".
     */
    
    @Override
    public String getName() {
        return "SpearmanCorrelation";
    }



    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        if (expected.isEmpty() || result.isEmpty()) {
            return (expected.isEmpty() && result.isEmpty()) ? 1.0 : 0.0;
        }

        // 1. Map IDs to Scores (Assuming Col 0 is ID, Col 1 is Score)
        Map<String, Double> expectedMap = extractScoreMap(expected);
        Map<String, Double> resultMap = extractScoreMap(result);

        // 2. Find Common IDs (Alignment)
        List<Double> alignedExpected = new ArrayList<>();
        List<Double> alignedResult = new ArrayList<>();

        for (String id : expectedMap.keySet()) {
            if (resultMap.containsKey(id)) {
                alignedExpected.add(expectedMap.get(id));
                alignedResult.add(resultMap.get(id));
            }
        }

        // Need at least 2 common pairs to calculate correlation
        if (alignedExpected.size() < 2) return 0.0;

        // 3. Convert to Ranks and Calculate
        double[] expectedRanks = calculateRanks(alignedExpected);
        double[] resultRanks = calculateRanks(alignedResult);

        return computePearson(expectedRanks, resultRanks);
    }

    private Map<String, Double> extractScoreMap(List<Tuple> tuples) {
        Map<String, Double> map = new HashMap<>();
        for (Tuple t : tuples) {
            List<Cell> cells = t.getCells();
            if (cells.size() < 2) continue;
            
            String id = cells.get(0).getValue().toString();
            String scoreStr = cells.get(1).getValue().toString();
            
            try {
                // Handle SQL/Python NaN and different decimal separators
                if (scoreStr == null || scoreStr.equalsIgnoreCase("nan")) continue;
                double score = Double.parseDouble(scoreStr.replace(",", "."));
                map.put(id, score);
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        return map;
    }

    private double[] calculateRanks(List<Double> values) {
        int n = values.size();
        double[] ranks = new double[n];
        Integer[] indices = IntStream.range(0, n).boxed().toArray(Integer[]::new);

        Arrays.sort(indices, Comparator.comparingDouble(values::get));

        for (int i = 0; i < n; i++) {
            int j = i + 1;
            while (j < n && values.get(indices[j]).equals(values.get(indices[i]))) {
                j++;
            }
            double avgRank = (i + 1 + j) / 2.0; // Tie handling
            for (int k = i; k < j; k++) {
                ranks[indices[k]] = avgRank;
            }
            i = j - 1;
        }
        return ranks;
    }

    private double computePearson(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i]; sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i]; sumY2 += y[i] * y[i];
        }
        double num = (n * sumXY) - (sumX * sumY);
        double den = Math.sqrt(((n * sumX2) - (sumX * sumX)) * ((n * sumY2) - (sumY * sumY)));
        return (den == 0) ? 0.0 : num / den;
    }
}