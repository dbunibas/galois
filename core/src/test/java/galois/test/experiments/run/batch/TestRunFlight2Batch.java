package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.llm.algebra.LLMScan;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.optimizer.QueryPlan;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.OptimizersFactory;
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
public class TestRunFlight2Batch {

    private static final String EXP_NAME = "FLIGHT2";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "flight2-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";


    public TestRunFlight2Batch() {
        List<String> singleConditionOptimizers = List.of(
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        List<String> multipleConditionsOptimizers = List.of(
                "SingleConditionsOptimizerFactory",
                "SingleConditionsOptimizerFactory-WithFilter",
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );

        ExpVariant q1 = ExpVariant.builder()
                .queryNum("Q1")
                .querySql("SELECT call_sign FROM target.usa_airline_companies WHERE airline='jetblue airways'")
                .prompt("What is the call sign of Airline 'JetBlue Airways'?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT count(*) FROM target.usa_flights f")
                .prompt("How many flights do we have in US?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT a.airline FROM target.usa_airline_companies a WHERE a.call_sign='ual'")
                .prompt("Which airline has call sign 'ual'?")
                .optimizers(singleConditionOptimizers)
                .build();
        
        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT f.sourceairport, COUNT(f.flightno) as num_flights FROM target.usa_flights f WHERE f.destairport = 'LAX' GROUP BY f.sourceairport")
                .prompt("From which airports do flights arrive at LAX?")
                .optimizers(singleConditionOptimizers)
                .build();
        
//        ExpVariant q7 = ExpVariant.builder()
//                .queryNum("Q7")
//                .querySql("SELECT DISTINCT city FROM target.usa_airports WHERE airportname = 'municipal' AND city IS NOT NULL")
//                .prompt("List all distinct cities where airport name is 'municipal'.")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        // FIXME: Which Speedy tree can execute this query?
        
       variants = List.of(q1, q2, q3, q4);
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
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
    
    @Test
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = true;
        boolean execute = false;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            if (execute) testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/flight_2_data/flight_2-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
            String configPathKey = "/flight_2_data/flight_2-" + executorModel + "-key-scan-experiment.json";
            QueryPlan planEstimation = testRunner.planEstimation(configPathTable, variant); // it doesn't matter
            log.info("Plan Estimated: {}", planEstimation);
            String pushDownStrategy = planEstimation.computePushdown();
            Double confidenceKeys = planEstimation.getConfidenceKeys();
            Integer indexPushDown = planEstimation.getIndexPushDown();
            IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer"); //remove algebra false
            IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
            IOptimizer singleConditionPushDownRemoveAlgebraTree = null;
            IOptimizer singleConditionPushDown = null;
            if (indexPushDown != null) {
                singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexPushDown, true);
                singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexPushDown, false);
            }
            IOptimizer optimizer = null;
            if (pushDownStrategy.equals(QueryPlan.PUSHDOWN_ALL_CONDITION)) {
//                optimizer = allConditionPushdown;
                optimizer = allConditionPushdownWithFilter;
            }
            if (pushDownStrategy.startsWith(QueryPlan.PUSHDOWN_SINGLE_CONDITION)) {
//                optimizer = singleConditionPushDown;
                optimizer = singleConditionPushDownRemoveAlgebraTree;
            }
            if (executeAllPlans) {
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                IOptimizer optimizerAll = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizerAll);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, optimizerAll);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                }
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
    
    @Test
    public void testAllConditionPushDown() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer optimizer = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        String configPathTable = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
        String configPathKey = "/flight_2_data/flight_2-" + executorModel + "-key-scan-experiment.json";
        for (ExpVariant variant : variants) {
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, optimizer);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

    @Test
    public void testSingle() {
        // TO DEBUG single experiment
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        ExpVariant variant = variants.get(0);
        String configPath = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
        String type = "TABLE";
        int indexSingleCondition = 0;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, singleConditionPushDown);
    }
    
    @Test
    public void testConfidenceEstimatorSchema() {
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
//            break;
        }
    }
    
    @Test
    public void testCardinalityEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/flight_2_data/flight_2-" + executorModel + "-table-experiment.json";
            testRunner.executeCardinalityEstimatorQuery(configPath, variant);
//            break;
        }
    }

}
