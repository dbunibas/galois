package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.GaloisPipeline;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.utils.QueryUtils;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.optimizer.QueryPlan;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.utils.Configuration;
import galois.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Key;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunUSAPresidentsBatch {

    private static final String EXP_NAME = "USA_PRESIDENTS";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "usa-presidents-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "togetherai";
//    private String executorModel = "" + executorModel + "";

    public TestRunUSAPresidentsBatch() {
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
                .querySql("SELECT p.name, p.party FROM target.world_presidents p WHERE p.country='United States'")
//                .querySql("SELECT p.name, p.party FROM target.usa_presidents p WHERE p.country='United States'")
                .prompt("List the name and party of USA presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
//                .querySql("SELECT p.name, p.party FROM target.usa_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name and party of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
//                .querySql("SELECT count(p.party) FROM target.usa_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("Count the number of US presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
//                .querySql("SELECT p.name FROM target.usa_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican' AND p.start_year > 1980")
//                .querySql("SELECT p.name FROM target.usa_presidents p WHERE p.country='United States' AND p.party='Republican' AND p.start_year > 1980")
                .prompt("List the name of USA presidents after 1980 where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party FROM target.world_presidents p WHERE p.country='United States'")
//                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party FROM target.usa_presidents p WHERE p.country='United States'")
                .prompt("List the name, the start year, the end year, the number of president and the party of USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num FROM target.world_presidents p WHERE p.country='United States' group by p.party order by num desc limit 1")
//                .querySql("SELECT p.party, count(p.party) num FROM target.usa_presidents p WHERE p.country='United States' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) FROM target.world_presidents p WHERE p.country='United States' AND p.start_year >= 1990  AND p.start_year < 2000")
//                .querySql("SELECT count(*) FROM target.usa_presidents p WHERE p.country='United States' AND p.start_year >= 1990  AND p.start_year < 2000")
                .prompt("count U.S. presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Whig' order by p.end_year desc limit 1")
//                .querySql("SELECT p.name FROM target.usa_presidents p WHERE p.country='United States' AND p.party='Whig' order by p.end_year desc limit 1")
                .prompt("List the name of the last USA president where party is Whig")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1850 AND end_year < 1900 AND party ='Democratic'")
//                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.usa_presidents p WHERE p.country ='United States' AND start_year > 1850 AND end_year < 1900 AND party ='Democratic'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Democratic USA president who served between 1850 and 1900")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 AND party ='Democratic'")
//                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.usa_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 AND party ='Democratic'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Democratic USA president who served between 1850 and 1900")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q12 = ExpVariant.builder()
                .queryNum("Q12")
                .querySql("SELECT party, count(*) num FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1800 AND end_year < 1900 group by p.party order by num desc")
//                .querySql("SELECT party, count(*) num FROM target.usa_presidents p WHERE p.country ='United States' AND start_year > 1800 AND end_year < 1900 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a USA president between the 1800 and 1900.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q13 = ExpVariant.builder()
                .queryNum("Q13")
                .querySql("SELECT party, count(*) num FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 group by p.party order by num desc")
//                .querySql("SELECT party, count(*) num FROM target.usa_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a USA president between the 1900 and 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13);
    }

    @Test
    public void testCanParseSQLQueries() {
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        for (ExpVariant variant : variants) {
            log.info("Parsing query {}", variant.getQueryNum());
            assertDoesNotThrow(() -> {
                IAlgebraOperator result = sqlQueryParser.parse(variant.getQuerySql());
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
        String fileName = exportExcel.getFileName(EXP_NAME + "-" + provider);

        for (ExpVariant variant : variants) {
            String configPathNl = "/presidents/presidents-" + provider + "-nl-experiment.json";
            String configPathSql = "/presidents/presidents-" + provider + "-sql-experiment.json";
            String configPathTable = "/presidents/presidents-" + provider + "-table-experiment.json";
            String configPathKeyScan = "/presidents/presidents-" + provider + "-key-scan-experiment.json";

            testRunner.execute(configPathNl, "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute(configPathSql, "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
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
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
//            testRunner.execute("/presidents/presidents-togetherai-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/presidents/presidents-togetherai-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/presidents/presidents-togetherai-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-" + executorModel + "-key-experiment.json", "ORIGINAL-GALOIS", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/presidents/presidents-togetherai-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
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
            if (execute)
                testRunner.execute("/presidents/presidents-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute)
                testRunner.execute("/presidents/presidents-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/presidents/presidents-" + executorModel + "-table-experiment.json";
            String configPathKey = "/presidents/presidents-" + executorModel + "-key-scan-experiment.json";
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
                if (execute)
                    testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                if (execute)
                    testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                IOptimizer allCondition = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
                if (execute)
                    testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allCondition);
                if (execute)
                    testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allCondition);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute)
                        testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute)
                        testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
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
        String configPathTable = "/presidents/presidents-togetherai-table-experiment.json";
        String configPathKey = "/presidents/presidents-togetherai-key-scan-experiment.json";
        for (ExpVariant variant : variants) {
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, optimizer);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
    
    @Test
    public void testLlamaLogProbs() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer optimizer = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        String configPathTable = "/presidents/presidents-" + executorModel + "-table-experiment.json";
        String configPathKey = "/presidents/presidents-" + executorModel + "-key-scan-experiment.json";
//        Double thresholds[] = {0.5, 0.6, 0.7, 0.8, 0.9, 0.99, 0.995};
        Double thresholds[] = {0.9995, 0.99995};
        for (Double threshold : thresholds) {
            metrics = new ArrayList<>();
            results = new HashMap<>();
            for (ExpVariant variant : variants) {
                testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer, threshold);
                exportExcel.export(fileName+threshold, EXP_NAME, metrics, results);
            }
        }
        //Double threshold = 0.99;
        //ExpVariant variant =  variants.get(12);
        //testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer, threshold);
        //exportExcel.export(fileName, EXP_NAME, metrics, results);
    }
    
    @Test
    public void testLLamaLogProbsStaticResults() {
//        List<IMetric> metrics = new ArrayList<>();
//        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        String configPath = "/presidents/presidents-togetherai-table-experiment.json";
        String type = "TABLE";
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        ExpVariant variant = variants.get(12);
        Double thresholds[] = {0.9999, 0.99999, 0.999999};
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
    public void testIterationsImpact() {
        String configPathTable = "/presidents/presidents-" + executorModel + "-table-experiment.json";
        String configPathKey = "/presidents/presidents-" + executorModel + "-key-scan-experiment.json";
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
    public void testSingle() {
        // TO DEBUG single experiment
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        ExpVariant variant = variants.get(0);
        String configPath = "/presidents/presidents-togetherai-table-experiment.json";
        String type = "TABLE";
        int indexSingleCondition = 0;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
//        testRunner.executeSingle(configPath, type, variant, metrics, results, allConditionPushdownWithFilter);
        testRunner.executeSingle(configPath, type, variant, metrics, results, allConditionPushdownWithFilter, 0.8);

    }

    @Test
    public void testConfidenceEstimator() {
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/presidents/presidents-togetherai-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }

    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/presidents/presidents-togetherai-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
//            break;
        }
    }

    @Test
    public void testCardinalityEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/presidents/presidents-togetherai-table-experiment.json";
            testRunner.executeCardinalityEstimatorQuery(configPath, variant);
//            break;
        }
    }

    @Test
    public void testRunExperiment() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/presidents/presidents-togetherai-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-togetherai-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String tableExp = "/presidents/presidents-togetherai-table-experiment.json";
            String keyExp = "/presidents/presidents-togetherai-key-scan-experiment.json";
            //            Double popularity = getPopularity(tableExp, "usa_state", variant);
            Double popularity = testRunner.getPopularity(tableExp, variant);
            if (popularity >= 0.7) {
                log.info("Run KEY-CAN");
//                IOptimizer optimizer = testRunner.getOptimizerBasedOnCardinality(tableExp, variant);
                IOptimizer optimizer = testRunner.getOptimizerBasedOnLLMOptimization(tableExp, variant);
                testRunner.executeSingle(keyExp, "KEY-SCAN", variant, metrics, results, optimizer);
//                testRunner.execute(keyExp, "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            } else {
                log.info("Run TABLE");
//                IOptimizer optimizer = testRunner.getOptimizerBasedOnCardinality(tableExp, variant);
                IOptimizer optimizer = testRunner.getOptimizerBasedOnLLMOptimization(tableExp, variant);
                testRunner.executeSingle(tableExp, "TABLE", variant, metrics, results, optimizer);
//                testRunner.execute(tableExp, "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    private Double getPopularity(String expPath, String tableName, ExpVariant variant) {
        Experiment experiment = null;
        try {
            experiment = ExperimentParser.loadAndParseJSON(expPath);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return -1.0;
        }
        IDatabase database = experiment.getQuery().getDatabase();
        ITable table = database.getTable(tableName);
        List<Key> keys = database.getPrimaryKeys();
        Key key = keys.get(0);
        String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(variant.getQuerySql());
        String whereExpression = parserWhere.getWhereExpression();
        String prompt = "Given the following JSON schema:\n";
        prompt += jsonSchema + "\n";
        prompt += "What is the popularity in your knowledge of " + key.toString() + " of " + tableName;
        if (whereExpression != null && !whereExpression.trim().isEmpty()) {
            prompt += " where " + whereExpression;
        }
        prompt += "?\n";
        prompt += "Return a value between 0 and 1. Where 1 is very popular and 0 is not popular at all.\n"
                + "Respond with JSON only with a numerical property with name \"popularity\".";
        TogetherAIModel model = new TogetherAIModel(Configuration.getInstance().getTogetheraiApiKey(), TogetherAIConstants.MODEL_LLAMA3_8B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        String cleanResponse = Mapper.toCleanJsonObject(response);
        Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        Double popularity = (Double) parsedResponse.getOrDefault("popularity", -1.0);
        return popularity;
    }

    @Test
    public void testCardinalityRun() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer n = null;
        IOptimizer a = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IOptimizer> optimizers = Arrays.asList(a, a, a, a, a, n, n, n, a, a, a, a, n);
        String configPathTable = "/presidents/presidents-" + executorModel + "-table-experiment.json";
        String configPathKey = "/presidents/presidents-" + executorModel + "-key-scan-experiment.json";
        int i = 0;
        for (ExpVariant variant : variants) {
            IOptimizer o = optimizers.get(i);
            testRunner.executeSingle(configPathTable, "TABLE-CARDINALITY", variant, metrics, results, o);
//            testRunner.executeSingle(configPathKey, "KEY-SCAN-CARDINALITY", variant, metrics, results, o);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
}
