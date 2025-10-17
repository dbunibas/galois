package bsf.test.experiments.metrics;


import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;

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
                .map(tuple -> tuple.getCells().get(1).getValue().toString())
                .map(cellNormalizer::normalize)
                .distinct()
                .toList();

        List<String> resultCells = result.stream()
                .map(tuple -> tuple.getCells().get(1).getValue().toString())
                .map(cellNormalizer::normalize)
                .distinct()
                .toList();

        if (expectedCells.isEmpty()) {
            return 0.0;
        }

        double rho;
        int n = Math.max(expectedCells.size(), 2);
        double[] targetRanks = new double[expectedCells.size()];
        double[] predRanks = new double[expectedCells.size()];

        // Calculate ranks
        for (int i = 0; i < expectedCells.size(); i++) {
            String cell = expectedCells.get(i);
            targetRanks[i] = i;
            predRanks[i] = resultCells.indexOf(cell);
        }

        // Compute similarity based on the Spearman rank correlation coefficient
        double sumDiffRankSquared = 0;
        for (int i = 0; i < expectedCells.size(); i++) {
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
