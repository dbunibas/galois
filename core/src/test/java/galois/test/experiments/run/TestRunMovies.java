package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestRunMovies {

    @Test
    public void testExp() {
//        execute("/movies/movies-llama3-nl-experiment.json");
//        execute("/movies/movies-llama3-sql-experiment.json");
//        execute("/movies/movies-llama3-table-experiment.json");
        execute("/movies/movies-llama3-key-experiment.json");
//        execute("/movies/movies-llama3-key-scan-experiment.json");
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
