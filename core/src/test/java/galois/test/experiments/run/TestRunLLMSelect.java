package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunLLMSelect {
    @Test
    public void testDummyExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/dummy-continents/dummy-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }
}
