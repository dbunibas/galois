package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunGeo {

    @Test
    public void testOllamaLlama3NLExperiment() {
        execute("/geo_data/geo-llama3-nl-experiment.json");
    }

    @Test
    public void testOllamaLlama3SQLExperiment() {
        execute("/geo_data/geo-llama3-sql-experiment.json");
    }

    @Test
    public void testOllamaLlama3TableExperiment() {
        execute("/geo_data/geo-llama3-table-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyExperiment() {
        execute("/geo_data/geo-llama3-key-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyScanExperiment() {
        execute("/geo_data/geo-llama3-key-scan-experiment.json");
    }

    private void execute(String path) {
        try {
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            var results = experiment.execute();
            log.info("Results: {}", results);
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
    }
}
