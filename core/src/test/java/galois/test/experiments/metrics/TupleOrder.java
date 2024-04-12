package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.*;

public class TupleOrder implements IMetric {

    @Override
    public String getName() {
        return "TupleOrderTag";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        CellNormalizer cellNormalizer = new CellNormalizer();

        List<String> expectedCells = expected.stream()
                .map(tuple -> tuple.getCells().get(1))
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .toList();

        List<String> resultCells = result.stream()
                .map(tuple -> tuple.getCells().get(1))
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .toList();

        // remove duplicates mantaining the order
        List<String> newExpected = new ArrayList<>();
        for (String cell : expectedCells) {
            if (!newExpected.contains(cell)) {
                newExpected.add(cell);
            }
        }
        List<String> newResult = new ArrayList<>();
        for (String cell : resultCells) {
            if (!newResult.contains(cell)) {
                newResult.add(cell);
            }
        }
        if (newExpected.isEmpty()) {
            return 0.0;
        }

        double rho;
        int n = newExpected.size() > 1 ? newExpected.size() : 2;
        double[] targetRanks = new double[newExpected.size()];
        double[] predRanks = new double[newExpected.size()];

        // Calculate ranks
        for (int i = 0; i < newExpected.size(); i++) {
            String cell = newExpected.get(i);
            targetRanks[i] = i;
            predRanks[i] = newResult.indexOf(cell);
        }

        // compute similarity based on the Spearman rank correlation coeff
        double sumDiffRankSquared = 0;
        for (int i = 0; i < newExpected.size(); i++) {
            sumDiffRankSquared += Math.pow(targetRanks[i] - predRanks[i], 2);
        }

        rho = 1 - 6 * sumDiffRankSquared / (n * (Math.pow(n, 2) - 1));

        return normalize(Math.round(rho * 1000.0) / 1000.0);
    }

    private double normalize(double data) {
        double[] dataArray = {-1, data, 1};
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double value : dataArray) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        return (dataArray[1] - min) / (max - min);
    }
}