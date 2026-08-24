package galois.test.experiments.metrics;

import lombok.Getter;
import speedy.model.database.Tuple;

import java.util.List;

public class TupleCellSimilarityCounter {
    @Getter
    private static final TupleCellSimilarityCounter instance = new TupleCellSimilarityCounter();

    private double precision = 0.0;
    private double recall = 0.0;
    private List<Tuple> expected = null;
    private List<Tuple> result = null;

    private TupleCellSimilarityCounter() {
    }

    void reset() {
        precision = 0.0;
        recall = 0.0;
        expected = null;
        result = null;
    }

    void setScores(double precision, double recall) {
        this.precision = precision;
        this.recall = recall;
    }

    void swapScores() {
        double swap = precision;
        precision = recall;
        recall = swap;
    }

    void bindTo(List<Tuple> expected, List<Tuple> result) {
        this.expected = expected;
        this.result = result;
    }

    public double getPrecision(List<Tuple> expected, List<Tuple> result) {
        return computedOn(expected, result) ? precision : Double.NaN;
    }

    public double getRecall(List<Tuple> expected, List<Tuple> result) {
        return computedOn(expected, result) ? recall : Double.NaN;
    }

    private boolean computedOn(List<Tuple> expected, List<Tuple> result) {
        return this.expected == expected && this.result == result;
    }
}
