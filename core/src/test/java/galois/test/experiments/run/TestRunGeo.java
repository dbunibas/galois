package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRunGeo {

    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "prova.txt";

//    @Test
//    public void testExperimentsSequentially() {
//        testOllamaLlama3NLExperiment();
//        testOllamaLlama3SQLExperiment();
//        testOllamaLlama3TableExperiment();
//        testOllamaLlama3KeyExperiment();
//        testOllamaLlama3KeyScanExperiment();
//    }

    @Test
    @Order(1)
    public void testOllamaLlama3NLExperiment() {
        execute("/geo_data/geo-llama3-nl-experiment.json");
    }

    @Test
    @Order(2)
    public void testOllamaLlama3SQLExperiment() {
        execute("/geo_data/geo-llama3-sql-experiment.json");
    }

    @Test
    @Order(3)
    public void testOllamaLlama3TableExperiment() {
        execute("/geo_data/geo-llama3-table-experiment.json");
    }

    @Test
    @Order(4)
    public void testOllamaLlama3KeyExperiment() {
        execute("/geo_data/geo-llama3-key-experiment.json");
    }

    @Test
    @Order(5)
    public void testOllamaLlama3KeyScanExperiment() {
        execute("/geo_data/geo-llama3-key-scan-experiment.json");
    }

    private void execute(String path) {
        try {
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            var results = experiment.execute();
            log.info("Results: {}", results);

            // Extract numbers after "Only results, same order: \n"
            String regex = "Only results, same order: \\n([\\d, ]+)-*$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(results.toString());

            if (matcher.find()) {
                String extractedResults = matcher.group(1).trim();;
                saveToFile(extractedResults, RESULT_FILE_DIR + RESULT_FILE);
            } else {
                log.error("Could not extract results from log entry.");
            }


        } catch (IOException ioe) {
            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
    }

    private static void saveToFile(String data, String fileName) {
        try {
            // Ensure the directory exists
            File directory = new File(RESULT_FILE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                writer.write(data);
                writer.newLine(); // Add a new line after each result for better readability
            }
        } catch (IOException e) {
            log.error("Error writing to file: " + fileName, e);
        }
    }
}
