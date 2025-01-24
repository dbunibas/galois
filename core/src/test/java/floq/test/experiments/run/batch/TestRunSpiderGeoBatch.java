package floq.test.experiments.run.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import engine.model.database.Attribute;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;
import floq.llm.models.togetherai.CellProb;
import floq.llm.models.togetherai.TupleProb;
import floq.optimizer.IOptimizer;
import floq.optimizer.IndexedConditionPushdownOptimizer;
import floq.optimizer.PhysicalPlanSelector;
import floq.optimizer.QueryPlan;
import floq.test.experiments.Experiment;
import floq.test.experiments.ExperimentResults;
import floq.test.experiments.json.parser.ExperimentParser;
import floq.test.experiments.json.parser.OptimizersFactory;
import floq.test.experiments.metrics.IMetric;
import floq.test.model.ExpVariant;
import floq.test.utils.ExcelExporter;
import floq.test.utils.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static engine.utility.EngineUtility.printMap;

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
    private String executorModel = "llama3";

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
                .querySql("SELECT population FROM usa_city WHERE city_name = 'tempe';")
                .prompt("how many people live in tempe")
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

//        ExpVariant q10 = ExpVariant.builder()
//                .queryNum("Q10")
//                .querySql("SELECT DISTINCT usa_state_traversed FROM usa_river;")
//                .prompt("which states have a river")
//                .optimizers(List.of())
//                .build();

        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
                //                .querySql("SELECT lake_name FROM usa_lake;")
                .querySql("SELECT DISTINCT lake_name FROM usa_lake WHERE area_squared_km > 450;")
                .prompt("name all the lakes of us")
                .optimizers(List.of())
                .build();

        ExpVariant q12 = ExpVariant.builder()
                .queryNum("Q12")
                //                .querySql("SELECT usa_state_traversed FROM usa_river WHERE length_in_km > 750;")
                .querySql("SELECT DISTINCT usa_state_traversed FROM usa_river WHERE length_in_km > 750;")
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

//        ExpVariant q18 = ExpVariant.builder()
//                .queryNum("Q18")
//                .querySql("SELECT COUNT ( usa_state_traversed ) FROM usa_river WHERE length_in_km > 750;")
//                .prompt("how many states have major rivers")
//                .optimizers(singleConditionOptimizers)
//                .build();

//        ExpVariant q19 = ExpVariant.builder()
//                .queryNum("Q19")
//                .querySql("SELECT usa_state_traversed FROM usa_river WHERE length_in_km = ( SELECT MIN ( length_in_km ) FROM usa_river);")
//                .prompt("what states does the shortest river run through")
//                .optimizers(singleConditionOptimizers)
//                .build(); // NESTED QUERY WE DON'T MANAGE IT!
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

//        ExpVariant q25 = ExpVariant.builder()
//                .queryNum("Q25")
//                .querySql("SELECT r.river_name FROM usa_river r WHERE r.length_in_km > 750.0 AND r.usa_state_traversed = 'illinois';")
//                .prompt("name the major rivers in illinois")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        ExpVariant q26 = ExpVariant.builder()
                .queryNum("Q26")
                //                .querySql("SELECT t2.capital FROM usa_state AS t2 JOIN usa_city AS t1 ON t2.state_name = t1.state_name WHERE t1.city_name = 'durham';")
                .querySql("SELECT t2.capital FROM usa_city AS t1 JOIN usa_state AS t2 ON t1.state_name = t2.state_name WHERE t1.city_name = 'tempe';")
                //                .prompt("name the major rivers") //? Incorrect
                .prompt("the capital of the state where there is tempe") //?
                .optimizers(singleConditionOptimizers)
                .build();

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
                //                .querySql("SELECT t2.capital FROM usa_state AS t2 JOIN usa_city AS t1 ON t2.capital = t1.city_name WHERE t1.population <= 150000;")
                .querySql("SELECT t2.capital FROM usa_city AS t1 JOIN usa_state AS t2 ON t1.city_name=t2.capital WHERE t1.population <= 150000;")
                .prompt("which capitals are not major cities")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q30 = ExpVariant.builder()
                .queryNum("Q30")
                .querySql("SELECT state_name, population, area_squared_miles FROM usa_state")
                .prompt("List the state name, population and area from USA states")
                .optimizers(List.of())
                .build();

        ExpVariant q31 = ExpVariant.builder()
                .queryNum("Q31")
                .querySql("SELECT us.state_name, us.capital, us.area_squared_miles FROM target.usa_state us")
                .prompt("List the state name, capital and area from USA states")
                .optimizers(List.of())
                .build();

        ExpVariant q32 = ExpVariant.builder()
                .queryNum("Q32")
                .querySql("SELECT state_name, population, area_squared_miles FROM usa_state WHERE capital = 'frankfort'")
                .prompt("List the state name, population and area from USA states where the capital is Frankfort")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q33 = ExpVariant.builder()
                .queryNum("Q33")
                .querySql("SELECT us.state_name, us.population, us.capital FROM usa_state us WHERE us.population > 5000000")
                .prompt("List the state name, population and capital from USA states where the population is greater than 5000000")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q34 = ExpVariant.builder()
                .queryNum("Q34")
                .querySql("SELECT us.state_name, us.capital FROM usa_state us WHERE us.population > 5000000 AND us.density < 1000")
                .prompt("List the state name and capital from USA states where the population is greater than 5000000 and the density is lower than 1000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q35 = ExpVariant.builder()
                .queryNum("Q35")
                .querySql("SELECT us.state_name, us.capital, us.density, us.population FROM usa_state us WHERE us.population > 5000000 AND us.density < 1000 AND us.area_squared_miles < 50000")
                .prompt("List the state name, capital, density and population from USA states where the population is greater than 5000000, the density is lower than 1000 and the area is lower than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q36 = ExpVariant.builder()
                .queryNum("Q36")
                .querySql("SELECT us.state_name, us.capital FROM usa_state us WHERE us.population > 3000000 AND us.area_squared_miles > 50000 order by us.capital")
                .prompt("List the state name and capital ordered by capital from USA states where the population is greater than 3000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q37 = ExpVariant.builder()
                .queryNum("Q37")
                .querySql("SELECT us.state_name, us.capital, us.population FROM usa_state us WHERE us.population > 3000000 AND us.population < 8000000 AND us.area_squared_miles > 50000 order by us.population")
                .prompt("List the state name capital and population ordered by population from USA states where the population is greater than 3000000 and lower than 8000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q38 = ExpVariant.builder()
                .queryNum("Q38")
                .querySql("SELECT us.state_name, us.capital, us.population, us.area_squared_miles FROM usa_state us WHERE us.population = 4700000 AND us.area_squared_miles=56153")
                .prompt("List the state name, the capital, the popoulation and the area from USA states where the population is 4700000 and the are is 56153")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ///// FOR TESTS
        ExpVariant q39 = ExpVariant.builder()
                .queryNum("Q39")
                .querySql("SELECT mountain_name, mountain_altitude_in_meters FROM usa_mountain WHERE country_name = 'united states' ORDER BY mountain_altitude_in_meters DESC LIMIT 10;")
                .prompt("What are the top 10 highest mountains in the United States?")
                .optimizers(multipleConditionsOptimizers)
                .build();


        ExpVariant q40 = ExpVariant.builder()
                .queryNum("Q40")
                .querySql("SELECT t1.state_name, COUNT(*) FROM usa_state as t1 JOIN usa_border_info as t2 ON t1.state_name = t2.state_name WHERE t1.area_squared_miles > 50000 AND t1.country_name='united states' GROUP BY t1.state_name;")
                .prompt("How many neighboring states does each state of united states with an area in squared miles greater than 50,000 have?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q41 = ExpVariant.builder()
                .queryNum("Q41")
                .querySql("SELECT mountain_name FROM usa_mountain WHERE state_name = 'alaska';")
                .prompt("What are the mountains in Alaska?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q42 = ExpVariant.builder()
                .queryNum("Q42")
                .querySql("SELECT lake_name, area_squared_km, state_name FROM usa_lake WHERE area_squared_km > 750;")
                .prompt("What are the lakes in the United States with an area greater than 750 squared km?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q43 = ExpVariant.builder()
                .queryNum("Q43")
                .querySql("SELECT COUNT ( river_name ) FROM usa_river WHERE length_in_km > 750 AND usa_state_traversed  = 'ohio';")
                .prompt("How many rivers in Ohio are longer than 750 km?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q44 = ExpVariant.builder()
                .queryNum("Q44")
                .querySql("SELECT state_name, country_name, mountain_altitude_in_meters FROM usa_mountain WHERE mountain_name = 'whitney';")
                .prompt("What state is Mount Whitney in?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q45 = ExpVariant.builder()
                .queryNum("Q45")
                .querySql("SELECT COUNT ( city_name ) FROM usa_city WHERE population  >  150000 AND state_name  = 'texas';")
                .prompt("How many cities in Texas have a population greater than 150,000?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q46 = ExpVariant.builder()
                .queryNum("Q46")
                .querySql("SELECT COUNT ( DISTINCT usa_state_traversed ) FROM usa_river WHERE length_in_km  >  750;")
                .prompt("How many distinct usa states are traversed by rivers longer than 750 km?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q47 = ExpVariant.builder()
                .queryNum("Q47")
                .querySql("SELECT SUM ( area_squared_miles ) FROM usa_state;")
                .prompt("What is the total area in squared miles of the United States?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q48 = ExpVariant.builder()
                .queryNum("Q48")
                .querySql("SELECT DISTINCT t1.capital, t1.state_name FROM usa_state AS t1 JOIN usa_city AS t2 ON t1.state_name  =  t2.state_name WHERE t1.area_squared_miles > 50000;")
                .prompt("What are the capitals of states with an area greater than 50,000 squared miles?")
                .optimizers(multipleConditionsOptimizers)
                .build();


        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q11, q12, q13, q14, q15, q16, q17, q20, q21, q22, q23, q24, q26, q29, q30, q31, q32, q33, q34, q35, q36, q37, q38);
//        variants = List.of(q39, q40, q41, q42, q43, q44, q45, q46, q47, q48);

    }

    @Test
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = true;
        boolean execute = true;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
//            if (execute) testRunner.execute("/SpiderGeo/geo-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            if (execute) testRunner.execute("/SpiderGeo/geo-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            String configPathKey = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
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
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-FLOQ", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-FLOQ", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allConditionPushdownWithFilter);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allConditionPushdownWithFilter);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-FLOQ", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute) testRunner.executeSingle(configPathTable, "TABLE-FLOQ", variant, metrics, results, optimizer);
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
        String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        String configPathKey = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
        for (ExpVariant variant : variants) {
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, optimizer);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, optimizer);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }
    
    @Test
    public void testExpOptimization() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer nullOptimizer = null;
        String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        for (ExpVariant variant : variants) {
            QueryPlan planEstimation = testRunner.planEstimation(configPathTable, variant); // it doesn't matter
            log.info("Plan Estimated: {}", planEstimation);
            IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer"); //remove algebra false
            IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
            IOptimizer singleConditionPushDownRemoveAlgebraTree = null;
            IOptimizer singleConditionPushDown = null;
            String pushDownStrategy = planEstimation.computePushdown();
            Integer indexPushDown = planEstimation.getIndexPushDown();
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
            testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allConditionPushdownWithFilter);
            testRunner.executeSingle(configPathTable, "TABLE-UNOPTIMIZED", variant, metrics, results, nullOptimizer);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

    @Test
    public void testLLamaLogProbsStaticResults() {
//        List<IMetric> metrics = new ArrayList<>();
//        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        String type = "TABLE";
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        ExpVariant variant = variants.get(7);
        Double thresholds[] = {0.5, 0.6, 0.7, 0.8, 0.9, 0.99, 0.995, 0.9999, 0.99999, 0.999999};
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
        String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        String configPathKey = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
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
        ExpVariant variant = variants.get(31);
        String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        String type = "TABLE";
//        String configPath = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
//        String type = "KEY-SCAN";
        int indexSingleCondition = 1;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, singleConditionPushDownRemoveAlgebraTree, 0.8);
    }

    @Test
    public void testConfidenceEstimatorSchema() {
        for (ExpVariant variant : variants) {
            String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }

    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
            String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }

    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
        }
    }

    @Test
    public void testCardinalityEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            testRunner.executeCardinalityEstimatorQuery(configPath, variant);
//            break;
        }
    }

    @Test
    public void testGalois() {
        double threshold = 0.9;
        boolean removeFromAlgebraTree = true;
        ExpVariant variantForConfidence = variants.get(0); //we need only one
        String configPath = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        Map<ITable, Map<Attribute, Double>> dbConfidence = testRunner.executeConfidenceEstimator(configPath, variantForConfidence);
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IDatabase database = null;
        try {
            Experiment experiment = ExperimentParser.loadAndParseJSON("/SpiderGeo/geo-" + executorModel + "-sql-experiment.json");
            database = experiment.getQuery().getDatabase();
        } catch (IOException ioe) {
            log.error("Unable to parse JSON to extract Database Object");
            return;
        }
        for (ExpVariant variant : variants) {
            // Baseline Execution
            testRunner.execute("/SpiderGeo/geo-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/SpiderGeo/geo-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            // Galois Execution
            PhysicalPlanSelector physicalPlanSelector = new PhysicalPlanSelector();
            String selectedPlan = physicalPlanSelector.getPlanByKeyStrategy(database, variant.getQuerySql());
            log.info("Query {} selected plan: {}", variant.getQuerySql(), selectedPlan);
            if (selectedPlan.equals(PhysicalPlanSelector.PLAN_TABLE)) {
                testRunner.executeGalois("/SpiderGeo/geo-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE, dbConfidence, threshold, removeFromAlgebraTree);
            }
            if (selectedPlan.equals(PhysicalPlanSelector.PLAN_KEY_SCAN)) {
                testRunner.executeGalois("/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE, dbConfidence, threshold, removeFromAlgebraTree);
            }
//            testRunner.execute("/SpiderGeo/geo" + executorModel + "key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
    
    @Test
    public void testCardinalityRun() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        IOptimizer n = null;
        IOptimizer a = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IOptimizer> optimizers = Arrays.asList(a, n, a, n, n, a, n, n, n, a, n, a, a, a, a, a, a, n, n, n, n, a, n, a, n, n, n, a, a, a, a, a);
        String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
            String configPathKey = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
        int i = 0;
        for (ExpVariant variant : variants) {
           IOptimizer o = optimizers.get(i);
            testRunner.executeSingle(configPathTable, "TABLE-CARDINALITY", variant, metrics, results, o);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

    @Test
    public void testIterations() {
        String configPathTable = "/SpiderGeo/geo-" + executorModel + "-table-experiment.json";
        String configPathKey = "/SpiderGeo/geo-" + executorModel + "-key-scan-experiment.json";
        double threshold = 0.6;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);

        List<ExpVariant> iterVariants = List.of(
                variants.get(7),
                variants.get(9),
                variants.get(11)
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

    @Test
    public void removeDuplicatesForProb() {
        String dbName = "spider-geo";
        String queries[] = {"Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", "Q8", "Q9", "Q11", "Q12", "Q13", "Q14", "Q15", "Q16", "Q17", "Q20", "Q21", "Q22", "Q23", "Q24", "Q30", "Q31", "Q32", "Q33", "Q34", "Q35", "Q36", "Q37", "Q38"};
        for (String queryN : queries) {
            String fileName = "~/floq/exp-logs-llama/" + dbName + "-" + queryN + ".json";
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<List<TupleProb>> listTuple = new TypeReference<>() {
            };
            List<Tuple> tuples = new ArrayList<>();
            Set<String> tupleAdded = new HashSet<>();
            try {
                List<TupleProb> tuplesProb = mapper.readValue(new File(fileName), listTuple);
                System.out.println("Query: " + queryN);
                System.out.println("Before: " + tuplesProb.size());
                List<TupleProb> noDuplicates = new ArrayList<>();
                for (TupleProb tp : tuplesProb) {
                    String tupleKey = tupleToString(tp);
                    if (!tupleAdded.contains(tupleKey)) {
                        noDuplicates.add(tp);
                        tupleAdded.add(tupleKey);
                    }
                }
                System.out.println("After: " + noDuplicates.size());
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(noDuplicates);
                FileUtils.writeStringToFile(new File(fileName), json, Charset.forName("UTF-8"));

            } catch (Exception e) {
                log.error("Unable to open file: {}", fileName);
                log.error("Exception in reading obj from JSON: {}", e);
            }
        }

    }

    private String tupleToString(TupleProb tp) {
        String tupleToString = "";
        for (CellProb cell : tp.getCells()) {
            tupleToString += cell.getAttributeName() + ":" + cell.getValue() + "|";
        }
        return tupleToString;
    }
}
