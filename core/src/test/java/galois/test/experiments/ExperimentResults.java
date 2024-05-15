package galois.test.experiments;

import galois.test.experiments.metrics.IMetric;
import lombok.Data;
import speedy.model.database.Tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class ExperimentResults {
    private final String name;
    private final List<IMetric> metrics;
    private final List<Tuple> expectedResults;
    private final List<Tuple> actualResults;
    private final List<Double> scores;
    private final String queryExecutor;
    private final String sql_query;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Expected results:\n").append(expectedResults).append("\n");
        sb.append("Actual results:\n").append(actualResults).append("\n");
        sb.append("Scores:\n");
        // Split the string by "." and take the last element
        String[] parts = queryExecutor.split("\\.");
        String executorName = parts[parts.length - 1];
        sb.append("Query Executor: ").append(executorName).append("\n");
        sb.append("SQL Query: ").append(sql_query).append("\n");
        for (int i = 0; i < scores.size(); i++) {
            sb.append(metrics.get(i).getName()).append(": ").append(scores.get(i)).append("\n");
        }

        // return sb.toString();
        sb.append("\n");
        sb.append("------------------------------------------------------------------------------------\n");
        String result = sb.toString();
        String nameReplaced = name.replace(" ", "_");
        String basePath = System.getProperty("user.dir");
        Path filePath = Paths.get(basePath, "src", "test", "resources", "results", nameReplaced + ".txt");

        try {
            Files.write(Paths.get(filePath.toUri()), result.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
