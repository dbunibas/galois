package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunVenezuelaPresidents {

    @Test
    public void TestOllamaLlama3NLExperiment() {execute("/presidents_venezuela/presidents-llama3-nl-experiment.json");}

    @Test
    public void TestOllamaLlama3SQLExperiment() {execute("/presidents_venezuela/presidents-llama3-sql-experiment.json");}

    @Test
    public void testOllamaLlama3TableExperiment() {
        execute("/presidents_venezuela/presidents-llama3-table-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyExperiment() {
        execute("/presidents_venezuela/presidents-llama3-key-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyScanExperiment() {
        execute("/presidents_venezuela/presidents-llama3-key-scan-experiment.json");
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
