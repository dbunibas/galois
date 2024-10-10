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
public class TestRunWorld1Batch {

    private static final String EXP_NAME = "WORLD1";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "world1-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunWorld1Batch() {
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

//        ExpVariant q1 = ExpVariant.builder()
//                .queryNum("Q1")
//                .querySql("SELECT c.name FROM target.city c WHERE c.population > 160000 AND c.population < 900000;")
//                .prompt("What are the cities whose population is between 160000 and 900000?")
//                .optimizers(singleConditionOptimizers)
//                .build();

//        ExpVariant q2 = ExpVariant.builder()
//                .queryNum("Q2")
//                .querySql("select sum(surface_area_in_km2) from target.country where region='Caribbean';")
//                .prompt("What is the total surface area of the countries in the Caribbean region?")
//                .optimizers(singleConditionOptimizers)
//                .build();
//
//        ExpVariant q3 = ExpVariant.builder()
//                .queryNum("Q3")
//                .querySql("select distinct country_code_3_letters from target.country_language where language != 'English';")
//                .prompt("What are the country codes of countries where people use languages other than English?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();
//
//        ExpVariant q4 = ExpVariant.builder()
//                .queryNum("Q4")
//                .querySql("select sum(population), max(gnp) from target.country where continent='Asia';")
//                .prompt("What is the total population and maximum GNP in Asia?")
//                .optimizers(singleConditionOptimizers)
//                .build();
//
//        ExpVariant q5 = ExpVariant.builder()
//                .queryNum("Q5")
//                .querySql("select sum(population) from target.city where district = 'Gerderland';")
//                .prompt("How many people live in Gelderland district?")
//                .optimizers(singleConditionOptimizers)
//                .build();

//        ExpVariant q6 = ExpVariant.builder()
//                .queryNum("Q6")
//                .querySql("select avg(life_expectancy) from target.country where continent = 'Africa' and government_form = 'Republic';")
//                .prompt("What is the average life expectancy in African countries that are republics?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT count(distinct government_form) FROM target.country WHERE continent = 'Africa';")
                .prompt("How many type of governments are in Africa?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT name FROM target.country WHERE independence_year > 1950;")
                .prompt("What are the names of countries that became independent after 1950?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT avg(gnp), sum(population) FROM target.country WHERE government_form = 'US Territory';")
                .prompt("What is the average GNP and total population in all nations whose government is US territory?")
                .optimizers(singleConditionOptimizers)
                .build();

        // CANNOT BE RUN DUE TO PERCENTAGE FROM TABLE 2
//        ExpVariant q10 = ExpVariant.builder()
//                .queryNum("Q10")
//                .querySql("SELECT T2.Language FROM country AS T1 JOIN country_language AS T2 ON T1.code_3_letters = T2.country_code_3_letters WHERE T1.Name = 'Aruba' ORDER BY Percentage DESC LIMIT 1;")
//                .prompt("Which language is the most popular in Aruba?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
//              .querySql("SELECT distinct t1.region FROM country as t1 join country_language as t2 on t1.code_3_letters = t2.country_code_3_letters WHERE t2.language = 'English' or t2.language = 'Dutch';")
                .querySql("SELECT distinct t2.region FROM country_language as t1 join country as t2 on t1.country_code_3_letters = t2.code_3_letters WHERE t1.language = 'English' or t1.language = 'Dutch';")
                .prompt("What are the regions that use English or Dutch?")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        ExpVariant q12 = ExpVariant.builder()
//                .queryNum("Q12")
//                .querySql("SELECT t2.language FROM country as t1 join country_language as t2 on t1.code_3_letters = t2.country_code_3_letters WHERE t1.head_of_state = 'Beatrix' and t2.is_official = 'T';")
//                .prompt("What is the official language spoken in the country whose head of state is Beatrix?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        ExpVariant q13 = ExpVariant.builder()
                .queryNum("Q13")
//              .querySql("SELECT count(distinct t1.continent) FROM country as t1 join country_language as t2 on t1.code_3_letters = t2.country_code_3_letters WHERE t2.language = 'Chinese';")
                .querySql("SELECT count(distinct t2.continent) FROM country_language as t1 join country as t2 on t1.country_code_3_letters = t2.code_3_letters WHERE t1.language = 'Chinese';")
                .prompt("What is the number of distinct continents where Chinese is spoken?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q14 = ExpVariant.builder()
                .queryNum("Q14")
//                .querySql("SELECT region FROM country as t1 join city as t2 on t1.code_3_letters = t2.country_code_3_letters WHERE t2.name = 'Kabul';")
                .querySql("SELECT t2.region FROM city as t1 join country as t2 on t1.country_code_3_letters = t2.code_3_letters WHERE t1.name = 'Kabul';")
                .prompt("Which region is the city Kabul located in?")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        ExpVariant q15 = ExpVariant.builder()
//                .queryNum("Q15")
//                .querySql("SELECT count(*) FROM country as t1 join country_language as t2 on t1.code_3_letters = t2.country_code_3_letters WHERE t1.name = 'Afghanistan' and is_official = 'T';")
//                .prompt("How many official languages does Afghanistan have?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        variants = List.of(  q7, q8, q9, q10, q11, q12, q13, q14, q15);
        variants = List.of(q7, q8, q9, q11);
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
            testRunner.execute("/world_1_data/world1-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/world_1_data/world1-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
    
    @Test
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = true;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
//            testRunner.execute("/world_1_data/world1-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/world_1_data/world1-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/world_1_data/world1-llama3-table-experiment.json";
            String configPathKey = "/world_1_data/world1-llama3-key-scan-experiment.json";
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
//                testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
//                testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
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
        String configPathTable = "/world_1_data/world1-llama3-table-experiment.json";
        String configPathKey = "/world_1_data/world1-llama3-key-scan-experiment.json";
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
        String configPath = "/world_1_data/world1-llama3-table-experiment.json";
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
            String configPath = "/world_1_data/world1-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/world_1_data/world1-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/world_1_data/world1-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
//            break;
        }
    }
    
    @Test
    public void testCardinalityEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/world_1_data/world1-llama3-table-experiment.json";
            testRunner.executeCardinalityEstimatorQuery(configPath, variant);
//            break;
        }
    }

}
