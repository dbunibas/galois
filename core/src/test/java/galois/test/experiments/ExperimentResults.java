package galois.test.experiments;

import galois.test.experiments.metrics.IMetric;
import lombok.Data;
import speedy.model.database.Tuple;

import java.util.List;

@Data
public class ExperimentResults {
    private final String name;
    private final List<IMetric> metrics;
    private final List<Tuple> expectedResults;
    private final List<Tuple> actualResults;
    private final List<Double> scores;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Expected results:\n").append(expectedResults).append("\n");
        sb.append("Actual results:\n").append(actualResults).append("\n");
        sb.append("Scores:\n");
        for (int i = 0; i < scores.size(); i++) {
            sb.append(metrics.get(i).getName()).append(": ").append(scores.get(i)).append("\n");
        }
        return sb.toString();
    }
}
