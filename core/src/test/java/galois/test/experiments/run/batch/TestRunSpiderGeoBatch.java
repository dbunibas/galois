package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.llm.models.IModel;
import galois.llm.models.TogetherAIModel;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
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
public class TestRunSpiderGeoBatch {

    // WE NEED TO KNOW THE UNIT MEASURES.
    // it seeme length is in KM
    private static final String EXP_NAME = "SPIDER-GEO";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "SpiderGeo-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunSpiderGeoBatch() {
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
                .querySql("SELECT area_squared_miles FROM target.usa_state WHERE state_name = 'new mexico';")
                .prompt("how big is new mexico")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT city_name FROM usa_city WHERE population > 150000;")
                .prompt("what are the major cities in the usa")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(capital) FROM usa_state WHERE state_name = 'rhode island';")
                .prompt("how many capitals does rhode island have")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT population FROM usa_city WHERE city_name = 'boulder';")
                .prompt("how many people live in boulder")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT count(state_name) FROM usa_state; ")
                .prompt("how many states are in the usa")
                .optimizers(List.of())
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                //                .querySql("SELECT lake_name FROM usa_lake WHERE state_name = 'california';")
                .querySql("SELECT lake_name FROM usa_lake WHERE state_name = 'california' and area_squared_km > 450;")
                //                .prompt("give me the lakes in california")
                .prompt("give me the major lakes in california")
                //                .optimizers(singleConditionOptimizers)
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT avg(population) FROM usa_state;")
                .prompt("what is the average population of the us by state")
                .optimizers(List.of())
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                //                .querySql("SELECT count(river_name) FROM usa_river;")
                .querySql("SELECT count(distinct river_name) FROM usa_river where length_in_km > 400;")
                .prompt("how many rivers are there in us")
                //                .optimizers(List.of())
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                //                .querySql("SELECT r.length_in_km FROM usa_river r WHERE r.river_name = 'rio grande';")
                .querySql("SELECT DISTINCT r.length_in_km FROM usa_river r WHERE r.river_name = 'rio grande';")
                .prompt("how long is the rio grande river")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("SELECT DISTINCT usa_state_traversed FROM usa_river;")
                .prompt("which states have a river")
                .optimizers(List.of())
                .build();

        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
                //                .querySql("SELECT lake_name FROM usa_lake;")
                .querySql("SELECT DISTINCT lake_name FROM usa_lake;")
                .prompt("name all the lakes of us")
                .optimizers(List.of())
                .build();

        ExpVariant q12 = ExpVariant.builder()
                .queryNum("Q12")
                .querySql("SELECT usa_state_traversed FROM usa_river WHERE length_in_km > 750;")
                .prompt("what states contain at least one major rivers")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q13 = ExpVariant.builder()
                .queryNum("Q13")
                //                .querySql("SELECT state_name FROM usa_city WHERE city_name = 'springfield';")
                .querySql("SELECT state_name FROM usa_city WHERE city_name = 'springfield' AND population > 62000;")
                .prompt("what state is springfield in")
                //                .optimizers(singleConditionOptimizers)
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q14 = ExpVariant.builder()
                .queryNum("Q14")
                .querySql("SELECT lake_name FROM usa_lake WHERE area_squared_km > 750 AND state_name = 'michigan';")
                .prompt("name the major lakes in michigan")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q15 = ExpVariant.builder()
                .queryNum("Q15")
                //                .querySql("SELECT lake_name FROM usa_lake WHERE area_squared_km > 750;")
                .querySql("SELECT DISTINCT lake_name FROM usa_lake WHERE area_squared_km > 750;")
                .prompt("what are the major lakes in united states")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q16 = ExpVariant.builder()
                .queryNum("Q16")
                .querySql("SELECT state_name FROM usa_city WHERE city_name = 'austin' AND population > 150000;")
                .prompt("which states have a major city named austin")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q17 = ExpVariant.builder()
                .queryNum("Q17")
                //                .querySql("SELECT COUNT ( state_name ) FROM usa_city WHERE city_name = 'springfield';")
                .querySql("SELECT COUNT ( DISTINCT state_name ) FROM usa_city WHERE city_name = 'springfield' AND population > 72000;")
                .prompt("how many states have a city named springfield")
                //                .optimizers(singleConditionOptimizers)
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q18 = ExpVariant.builder()
                .queryNum("Q18")
                .querySql("SELECT COUNT ( usa_state_traversed ) FROM usa_river WHERE length_in_km > 750;")
                .prompt("how many states have major rivers")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q19 = ExpVariant.builder()
                .queryNum("Q19")
                .querySql("SELECT usa_state_traversed FROM usa_river WHERE length_in_km = ( SELECT MIN ( length_in_km ) FROM usa_river);")
                .prompt("what states does the shortest river run through")
                .optimizers(singleConditionOptimizers)
                .build(); // NESTED QUERY WE DON'T MANAGE IT!

        ExpVariant q20 = ExpVariant.builder()
                .queryNum("Q20")
                .querySql("SELECT population FROM usa_city WHERE city_name = 'seattle' AND state_name = 'washington';")
                .prompt("what is the population of seattle washington")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q21 = ExpVariant.builder()
                .queryNum("Q21")
                .querySql("SELECT COUNT ( DISTINCT usa_state_traversed ) FROM usa_river WHERE length_in_km > 750;")
                .prompt("how many states are next to major rivers")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q22 = ExpVariant.builder()
                .queryNum("Q22")
                .querySql("SELECT DISTINCT capital FROM usa_state;")
                .prompt("name the capitals in the usa")
                .optimizers(List.of())
                .build();

        ExpVariant q23 = ExpVariant.builder()
                .queryNum("Q23")
                .querySql("SELECT COUNT ( river_name ) FROM usa_river WHERE usa_state_traversed = 'idaho';")
                .prompt("how many rivers are in idaho")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q24 = ExpVariant.builder()
                .queryNum("Q24")
                .querySql("SELECT country_name FROM usa_state WHERE state_name = 'massachusetts';")
                .prompt("where is massachusetts")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q25 = ExpVariant.builder()
                .queryNum("Q25")
                .querySql("SELECT r.river_name FROM usa_river r WHERE r.length_in_km > 750.0 AND r.usa_state_traversed = 'illinois';")
                .prompt("name the major rivers in illinois")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        ExpVariant q26 = ExpVariant.builder()
//                .queryNum("Q26")
//                .querySql("SELECT t2.capital FROM usa_state AS t2 JOIN usa_city AS t1 ON t2.state_name = t1.state_name WHERE t1.city_name = 'durham';")
//                .prompt("name the major rivers in illinois")
//                .optimizers(singleConditionOptimizers)
//                .build();
//        ExpVariant q27 = ExpVariant.builder()
//                .queryNum("Q27")
//                .querySql("SELECT state_name FROM state WHERE state_name NOT IN ( SELECT traverse FROM river );")
//                .prompt("what state has no rivers")
//                .optimizers(singleConditionOptimizers)
//                .build();
//        ExpVariant q28 = ExpVariant.builder()
//                .queryNum("Q28")
//                .querySql("SELECT COUNT ( river_name ) FROM river WHERE traverse NOT IN ( SELECT state_name FROM state WHERE capital = 'albany' );")
//                .prompt("how many rivers do not traverse the state with the capital albany")
//                .optimizers(singleConditionOptimizers)
//                .build();
        ExpVariant q29 = ExpVariant.builder()
                .queryNum("Q29")
                .querySql("SELECT t2.capital FROM usa_state AS t2 JOIN usa_city AS t1 ON t2.capital = t1.city_name WHERE t1.population <= 150000;")
                .prompt("which capitals are not major cities")
                .optimizers(singleConditionOptimizers)
                .build();

//        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10 ,q11, q12, q13, q14, q15, q16, q17, q18, q20, q21 , q22, q23, q24, q25, q29);
        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15, q16, q17, q18, q20, q21, q22, q23, q24, q25, q29);
//        variants = List.of(q8, q10,q12,q15, q18, q21, q22, q23, q25);
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
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/SpiderGeo/geo-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/SpiderGeo/geo-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/SpiderGeo/geo-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/SpiderGeo/geo-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/SpiderGeo/geo-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    @Test
    public void testSingle() {
        // TO DEBUG single experiment
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        ExpVariant variant = variants.get(19);
        String configPath = "/SpiderGeo/geo-llama3-nl-experiment.json";
        String type = "NL";
        int indexSingleCondition = 0;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, nullOptimizer);
    }

    @Test
    public void testConfidenceEstimator() {
        for (ExpVariant variant : variants) {
//            ExpVariant variant = variants.get(0);
            String configPath = "/SpiderGeo/geo-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
        }

    }
}
