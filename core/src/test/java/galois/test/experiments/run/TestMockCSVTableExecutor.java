package galois.test.experiments.run;

import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestMockCSVTableExecutor {
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "mock-csv-table-executor-results.txt";

    private static final TestRunner testRunner = new TestRunner();

    private final List<ExpVariant> variants;

    public TestMockCSVTableExecutor() {
        List<String> singleConditionOptimizers = List.of("AllConditionsPushdownOptimizer-WithFilter");

        ExpVariant q0 = ExpVariant.builder()
                .queryNum("Q0")
                .querySql("SELECT DISTINCT p.name, p.party FROM target.world_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name and party of Venezuela presidents.")
                .build();

        variants = List.of(q0);
    }

    @Test
    public void executeExperiments() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();

        for (ExpVariant variant : variants) {
            String configPathMock = "/presidents/presidents-mock-csv-table-executor-experiment.json";
            testRunner.execute(configPathMock, "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
        }

        log.info("Results\n{}", printMap(results));
    }
}
