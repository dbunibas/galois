package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunLLMSpider {
    @Test
    public void testContinentsLlama3Experiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/continents-llama3-table-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }
}
