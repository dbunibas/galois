package galois.test.experiments;

import galois.llm.query.LLMQueryStatManager;
import galois.test.experiments.metrics.IMetric;
import lombok.Data;
import speedy.model.database.Tuple;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@Data
@Slf4j
public class ExperimentResults {

    private final String name;
    private final List<IMetric> metrics;
    private final List<Tuple> expectedResults;
    private final List<Tuple> actualResults;
    private final List<Double> scores;
    private final String queryExecutor;
    private final String sql_query;
    private boolean exportActualResults = true;

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
        LLMQueryStatManager queryStats = LLMQueryStatManager.getInstance();
        int llmRequest = queryStats.getLLMRequest();
        double llmTokensInput = queryStats.getLLMTokensInput();
        double llmTokensOutput = queryStats.getLLMTokensOutput();
        long timeMs = queryStats.getTimeMs();
        double totalTokens = llmTokensInput + llmTokensOutput;
        sb.append("LLM Total Requests: ").append(llmRequest).append("\n");
        sb.append("LLM Total Input Tokens: ").append(llmTokensInput).append("\n");
        sb.append("LLM Total Output Tokens: ").append(llmTokensOutput).append("\n");
        sb.append("LLM Total Tokens: ").append(totalTokens).append("\n");
        sb.append("LLM Time (ms): ").append(timeMs).append("\n");
        // return sb.toString();
        sb.append("\n");
        sb.append("Only results, same order: \n");
        for (int i = 0; i < scores.size(); i++) {
            sb.append(scores.get(i)).append("\n");
        }
        sb.append(llmRequest).append("\n");
        sb.append(llmTokensInput).append("\n");
        sb.append(llmTokensInput).append("\n");
        sb.append(totalTokens).append("\n");
        sb.append(timeMs).append("\n");
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
        if (exportActualResults) {
            Path filePathCSV = Paths.get(basePath, "src", "test", "resources", "results", nameReplaced + ".csv");
            saveActualResult(filePathCSV);
        }
        return result;
    }

    private void saveActualResult(Path filePathCSV) {
        if (actualResults.isEmpty()) {
            return;
        }
        Tuple firstTuple = actualResults.get(0);
        if (firstTuple != null) {
            String[] headers = getHeaders(firstTuple);
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(headers).build();
            try {
                PrintWriter writer = new PrintWriter(filePathCSV.toFile());
                CSVPrinter printer = new CSVPrinter(writer, csvFormat);
                for (Tuple tuple : actualResults) {
                    printer.printRecord(getCellContent(tuple));
                }
                writer.close();
            } catch (IOException ioe) {
                log.error("Exception: {}", ioe);
        }
        }
    }

    private String[] getHeaders(Tuple tuple) {
        String[] headers = new String[tuple.getCells().size()];
        for (int i = 0; i < tuple.getCells().size(); i++) {
            headers[i] = tuple.getCells().get(i).getAttribute();
        }
        return headers;
    }

    private Object[] getCellContent(Tuple tuple) {
        String[] cells = new String[tuple.getCells().size()];
        for (int i = 0; i < tuple.getCells().size(); i++) {
            cells[i] = tuple.getCells().get(i).getValue().toString();
        }
        return cells;
    }
}
