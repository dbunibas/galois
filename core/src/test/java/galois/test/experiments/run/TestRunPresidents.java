package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;


@Slf4j
public class TestRunPresidents {

    @Test
    public void testExp() {
        execute("/presidents-usa/presidents-llama3-nl-experiment.json");
//        execute("/presidents-usa/presidents-llama3-sql-experiment.json");
//        execute("/presidents-usa/presidents-llama3-table-experiment.json");
//        execute("/presidents-usa/presidents-llama3-key-experiment.json");
//        execute("/presidents-usa/presidents-llama3-key-scan-experiment.json");
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
