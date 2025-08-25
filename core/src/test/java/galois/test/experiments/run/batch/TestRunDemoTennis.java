package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.GaloisPipeline;
import galois.llm.algebra.LLMScan;
import galois.optimizer.IOptimizer;
import galois.optimizer.QueryPlan;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunDemoTennis {
    private static final String EXP_NAME = "Web-Tennis";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "web-tennis-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunDemoTennis() {
        List<String> multipleConditionsOptimizers = List.of(
                "SingleConditionsOptimizerFactory",
                "SingleConditionsOptimizerFactory-WithFilter",
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );

        ExpVariant q1 = ExpVariant.builder()
                .queryNum("Q1")
                .querySql("select t.player_name, t.player_surname from wimbledon_winners t where t.tournament_year = 2023 and t.gender = 'male'")
                .prompt("List the player name and player surname of the male 2023 Wimbledon winner.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        variants = List.of(q1);
    }

    @Test
    public void testCanParseSQLQueries() {
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        for (ExpVariant variant : variants) {
            log.info("Parsing query {}", variant.getQueryNum());
            assertDoesNotThrow(() -> {
                IAlgebraOperator result = sqlQueryParser.parse(variant.getQuerySql(), ((tableAlias, attributes) -> new LLMScan(tableAlias, null, attributes, null)));
                log.info("Parsed result:\n{}", result);
            });
        }
    }

    @Test
    public void testRunBatchTogetherAI() {
        executeExperiments(Constants.PROVIDER_TOGETHERAI);
    }

    @Test
    public void testRunBatchOpenAI() {
        executeExperiments(Constants.PROVIDER_OPENAI);
    }

    private void executeExperiments(String provider) {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            String configPathTable = "/demo-tennis/demo-tennis-" + provider + "-table-experiment.json";
            String configPathKeyScan = "/demo-tennis/demo-tennis-" + provider + "-key-scan-experiment.json";

            testRunner.execute(configPathTable, "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute(configPathKeyScan, "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            executeFullPipeline(configPathTable, configPathKeyScan, variant, metrics, results);

            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    private void executeFullPipeline(String configPathTable, String configPathKeyScan, ExpVariant variant, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results) {
        double threshold = 0.6;
        GaloisPipeline pipeline = new GaloisPipeline();
        QueryPlan planEstimation = testRunner.planEstimation(configPathTable, variant);
        IOptimizer optimizer = pipeline.selectOptimizer(planEstimation, true);
        Double confidence = planEstimation.getConfidenceKeys();
        if (confidence != null && confidence > threshold) {
            // Execute KEY-SCAN
            testRunner.executeSingle(configPathKeyScan, "Galois Full (Key-Scan)", variant, metrics, results, optimizer);
        } else {
            // Execute TABLE
            testRunner.executeSingle(configPathTable, "Galois Full (Table)", variant, metrics, results, optimizer);
        }
    }

    @Test
    public void testRunContentRetriever() {
        ExpVariant qC = ExpVariant.builder()
                .queryNum("QC")
                .querySql("select t.player_name, t.player_surname from wimbledon_winners t where t.tournament_year = 2025 and t.gender = 'male'")
                .prompt("List the player name and player surname of the male 2025 Wimbledon winner.")
                .build();
        List<ExpVariant> variantsOverride = List.of(qC);

        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variantsOverride) {
            testRunner.execute("/demo-tennis/demo-tennis-togetherai-table-chroma-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
}
