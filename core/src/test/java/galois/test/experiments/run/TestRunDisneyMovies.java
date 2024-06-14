package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunDisneyMovies {

    @Test
    public void TestOllamaLlama3NLExperiment() {
        execute("/disney-movies/movies-llama3-nl-experiment.json");
    }

    @Test
    public void TestOllamaLlama3SQLExperiment() {
        execute("/disney-movies/movies-llama3-sql-experiment.json");
    }

    @Test
    public void testOllamaLlama3TableExperiment() {
        execute("/disney-movies/movies-llama3-table-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyExperiment() {
        execute("/disney-movies/movies-llama3-key-experiment.json");
    }

    @Test
    public void testOllamaLlama3KeyScanExperiment() {
        execute("/disney-movies/movies-llama3-key-scan-experiment.json");
    }

    @Test
    public void testExp() {
        execute("/disney-movies/movies-togetherai-llama3-nl-experiment.json");
//        execute("/disney-movies/movies-togetherai-llama3-table-experiment.json");
//        execute("/disney-movies/movies-togetherai-llama3-key-experiment.json");
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
