package galois.test.experiments;

import galois.llm.query.LLMQueryStatManager;
import galois.test.experiments.metrics.IMetric;
import java.io.File;

import galois.utils.Configuration;
import lombok.Data;
import speedy.model.database.Tuple;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final String optimizerName;
    private boolean exportActualResults = true;
    private boolean exportResults = true;
    private int llmRequest = LLMQueryStatManager.getInstance().getBaseLLMRequest() > 0 ? LLMQueryStatManager.getInstance().getBaseLLMRequest() : LLMQueryStatManager.getInstance().getLLMRequest();
    private double llmTokensInput = LLMQueryStatManager.getInstance().getBaseLLMTokensInput() > 0 ?  LLMQueryStatManager.getInstance().getBaseLLMTokensInput():  LLMQueryStatManager.getInstance().getLLMTokensInput();
    private double llmTokensOutput = LLMQueryStatManager.getInstance().getBaseLLMTokensOutput() > 0 ? LLMQueryStatManager.getInstance().getBaseLLMTokensOutput() : LLMQueryStatManager.getInstance().getLLMTokensOutput();
    private long timeMs = LLMQueryStatManager.getInstance().getBasetimeMs() > 0 ? LLMQueryStatManager.getInstance().getBasetimeMs() : LLMQueryStatManager.getInstance().getTimeMs();

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
        sb.append(llmTokensOutput).append("\n");
        sb.append(totalTokens).append("\n");
        sb.append(timeMs).append("\n");
        sb.append("------------------------------------------------------------------------------------\n");
        String result = sb.toString();
        String nameReplaced = name.replace(" ", "_");
//        String basePath = System.getProperty("user.dir");
//        Path filePath = Paths.get(basePath, "src", "test", "resources", "results", nameReplaced + ".txt");
        Path filePath = Paths.get(Configuration.getInstance().getResultsAbsolutePath(), nameReplaced + ".txt");
        try {
            Files.write(Paths.get(filePath.toUri()), result.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (exportActualResults) {
            Path filePathCSV = Paths.get(Configuration.getInstance().getResultsAbsolutePath(),nameReplaced + ".csv");
            saveActualResult(filePathCSV);
        }
        if (exportResults) {
            Path basePathExp = Paths.get(Configuration.getInstance().getResultsAbsolutePath());
            String pathExpected = basePathExp.toString() + File.separator + nameReplaced + "_expected.csv";
            String pathActual = basePathExp.toString() + File.separator + nameReplaced + "_actual.csv";
            if (optimizerName != null && !optimizerName.isEmpty()) {
                pathExpected = basePathExp.toString() + File.separator + nameReplaced + "_" + optimizerName + "_expected.csv";
                pathActual = basePathExp.toString() + File.separator + nameReplaced + "_" + optimizerName + "_actual.csv";
            }
            log.info("Export results in: " + pathExpected);
            log.info("Export actual in: " + pathActual);
            save(expectedResults, pathExpected);
            save(actualResults, pathActual);
        }
        return result;
    }
    
    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Expected results:\n").append(expectedResults).append("\n");
        sb.append("Actual results:\n").append(actualResults).append("\n");
        sb.append("Scores:\n");
        for (int i = 0; i < scores.size(); i++) {
            sb.append(metrics.get(i).getName()).append(": ").append(scores.get(i)).append("\n");
        }
        double totalTokens = llmTokensInput + llmTokensOutput;
        sb.append("LLM Total Requests: ").append(llmRequest).append("\n");
        sb.append("LLM Total Input Tokens: ").append(llmTokensInput).append("\n");
        sb.append("LLM Total Output Tokens: ").append(llmTokensOutput).append("\n");
        sb.append("LLM Total Tokens: ").append(totalTokens).append("\n");
        sb.append("LLM Time (ms): ").append(timeMs).append("\n");
        return sb.toString();
    }
    
    public Map<String,Double> getMetrics() {
        Map<String, Double> map = new HashMap<>();
        for(int i = 0; i < metrics.size(); i++) {
            String metricName = metrics.get(i).getName();
            try {
                double value = scores.get(i);
                map.put(metricName, value);
            } catch (Exception e) {
                map.put(metricName, 0.0);
            }
        }
        return map;
    }

    private void save(List<Tuple> tuples, String filePath) {
        if (tuples.isEmpty()) {
            return;
        }
        Tuple firstTuple = tuples.get(0);
        if (firstTuple != null) {
            String[] headers = getHeaders(firstTuple);
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(headers).build();
            try {
                PrintWriter writer = new PrintWriter(filePath);
                CSVPrinter printer = new CSVPrinter(writer, csvFormat);
                for (Tuple tuple : tuples) {
                    printer.printRecord(getCellContent(tuple));
                }
                writer.close();
            } catch (IOException ioe) {
                log.error("Exception: {}", ioe);
            }
        }

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
                File file = filePathCSV.toFile();
                file.getParentFile().mkdirs();
                PrintWriter writer = new PrintWriter(file);
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
