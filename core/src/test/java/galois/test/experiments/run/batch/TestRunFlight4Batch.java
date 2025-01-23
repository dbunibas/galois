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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunFlight4Batch {

    private static final String EXP_NAME = "FLIGHT4";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "flight4-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";

    public TestRunFlight4Batch() {
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
                .querySql("SELECT a.name FROM target.airports a WHERE a.elevation_in_ft >= -50 and a.elevation_in_ft <= 50")
                .prompt("Find the name of airports whose altitude is between -50 and 50")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT max(elevation_in_ft) FROM target.airports WHERE country = 'Iceland'")
                .prompt("What is the maximum elevation of all airports in the country of Iceland?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT name, city, country, elevation_in_ft FROM airports WHERE city = 'New York'")
                .prompt("Find the name, city, country, and altitude (or elevation) of the airports in the city of New York.")
                .optimizers(singleConditionOptimizers)
                .build();
        
        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT name, city, country FROM target.airports WHERE elevation_in_ft IS NOT NULL AND country = 'Brazil'")
                .prompt("Find the name, city, and country of airports in Brazil where the elevation is not null.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT DISTINCT country FROM target.airports WHERE elevation_in_ft > 1000 AND country IS NOT NULL")
                .prompt("Find all distinct countries that have airports with an elevation greater than 1000 feet.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT name, city, country, y FROM target.airports WHERE y IS NOT NULL ORDER BY y DESC LIMIT 5")
                .prompt("Find the name, city, country, and longitude of the 5 airports farthest to the east.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT name, city, country, elevation_in_ft FROM target.airports WHERE country = 'Canada' ORDER BY elevation_in_ft ASC, name ASC")
                .prompt("Find the name, city, country, and elevation of airports in Canada, ordered by elevation and name in ascending order.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        // FIXME: Which Speedy tree can execute this query?

        variants = List.of(q1, q2, q3);
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
//            testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-key-experiment.json", "ORIGINAL-GALOIS", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
    
    @Test
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = false;
        boolean execute = false;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            if (execute) testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/flight_4_data/flight_4-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
            String configPathKey = "/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json";
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
        String configPathTable = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String configPathKey = "/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json";
        for (ExpVariant variant : variants) {
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, optimizer);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
    
        @Test
    public void testLLamaLogProbsStaticResults() {
//        List<IMetric> metrics = new ArrayList<>();
//        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String type = "TABLE";
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        ExpVariant variant = variants.get(9);
        Double thresholds[] = {0.5, 0.6, 0.7, 0.8, 0.9, 0.99, 0.995};
        List<Double> precisions = new ArrayList<>();
        List<Double> recalls = new ArrayList<>();
        for (Double threshold : thresholds) {
            List<IMetric> metrics = new ArrayList<>();
            Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
            testRunner.executeSingle(configPath, type, variant, metrics, results, allConditionPushdownWithFilter, threshold);
            for (String queryNumber : results.keySet()) {
                Map<String, ExperimentResults> resultExp = results.get(queryNumber);
                for (String executedStrategy : resultExp.keySet()) {
                    ExperimentResults result = resultExp.get(executedStrategy);
                    Double precision = result.getMetrics().get("CellSimilarityPrecision");
                    Double recall = result.getMetrics().get("CellSimilarityRecall");
                    precisions.add(precision);
                    recalls.add(recall);
                }
            }
        }
        for (Double precision : precisions) {
            System.out.println(precision.toString().replace(".", ","));
        }
        System.out.println("");
        System.out.println("");
        for (Double recall : recalls) {
            System.out.println(recall.toString().replace(".", ","));
        }
    }

    @Test
    public void testSingle() {
        // TO DEBUG single experiment
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        ExpVariant variant = variants.get(1);
        String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String type = "TABLE";
        int indexSingleCondition = 0;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, allConditionPushdownWithFilter, 0.8);
    }
    
    @Test
    public void testIterationsImpact() {
        String configPathTable = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String configPathKey = "/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json";
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer optimizerAll = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        for (ExpVariant variant : variants) {
            testRunner.executeSingle(configPathTable, "TABLE-Unoptimized", variant, metrics, results, null);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-Unoptimized", variant, metrics, results, null);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-All", variant, metrics, results, optimizerAll);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
    
    @Test
    public void testConfidenceEstimatorSchema() {
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
//            break;
        }
    }
    
    @Test
    public void testCardinalityEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
            testRunner.executeCardinalityEstimatorQuery(configPath, variant);
//            break;
        }
    }
    
    @Test
    public void testCardinalityRun() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer n = null;
        IOptimizer a = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IOptimizer> optimizers = Arrays.asList(a, n, a);
        String configPathTable = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String configPathKey = "/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json";
        int i = 0;
        for (ExpVariant variant : variants) {
            IOptimizer o = optimizers.get(i);
            testRunner.executeSingle(configPathTable, "TABLE-CARDINALITY", variant, metrics, results, o);
//            testRunner.executeSingle(configPathKey, "KEY-SCAN-CARDINALITY", variant, metrics, results, o);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

    @Test
    public void testIterations() {
        String configPathTable = "/flight_4_data/flight_4-" + executorModel + "-table-experiment.json";
        String configPathKey = "/flight_4_data/flight_4-" + executorModel + "-key-scan-experiment.json";
        double threshold = 0.6;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);

        List<ExpVariant> iterVariants = List.of(
                variants.get(2)
        );

        for (ExpVariant variant: iterVariants) {
            IOptimizer optimizerAll = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer"); //remove algebra true
            QueryPlan planEstimation = testRunner.planEstimation(configPathTable, variant); // it doesn't matter
            log.info("Plan Estimated: {}", planEstimation);
            String pushDownStrategy = planEstimation.computePushdown();
            Double confidenceKeys = planEstimation.getConfidenceKeys();
            Integer indexPushDown = planEstimation.getIndexPushDown();
            IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
            IOptimizer singleConditionPushDownRemoveAlgebraTree = null;
            if (indexPushDown != null) {
                singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexPushDown, true);
            }
            IOptimizer optimizer = null;
            if (pushDownStrategy.equals(QueryPlan.PUSHDOWN_ALL_CONDITION)) {
                optimizer = allConditionPushdownWithFilter;
            }
            if (pushDownStrategy.startsWith(QueryPlan.PUSHDOWN_SINGLE_CONDITION)) {
                optimizer = singleConditionPushDownRemoveAlgebraTree;
            }
            testRunner.executeSingle(configPathTable, "FLOQ-A", variant, metrics, results, optimizerAll);
            if (confidenceKeys != null && confidenceKeys > threshold) {
                // Execute KEY-SCAN
                testRunner.executeSingle(configPathKey, "FLOQ-F", variant, metrics, results, optimizer);
            } else {
                // Execute TABLE
                testRunner.executeSingle(configPathTable, "FLOQ-F", variant, metrics, results, optimizer);
            }
        }

        exportExcel.export(fileName, EXP_NAME, metrics, results);
    }

}
