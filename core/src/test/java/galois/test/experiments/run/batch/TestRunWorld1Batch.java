package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
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

        ExpVariant q1 = ExpVariant.builder()
                .queryNum("Q1")
                .querySql("select c.Name from target.city c where c.Population > 160000 and c.Population < 900000")
                .prompt("What are the cities whose population is between 160000 and 900000?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("select sum(SurfaceArea) from target.country where region='Caribbean'")
                .prompt("What is the total surface area of the countries in the Caribbean region?")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("select distinct CountryCode from target.countrylanguage where language != 'English'")
                .prompt("What are the country codes of countries where people use languages other than English?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select sum(population), max(gnp) from target.country where continent='Asia'")
                .prompt("What is the total population and maximum GNP in Asia?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select sum(population) from target.city where district = 'Gerderland'")
                .prompt("How many people live in Gelderland district?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("select avg(LifeExpectancy) from target.country where continent = 'Africa' AND GovernmentForm = 'Republic'")
                .prompt("What is the average life expectancy in African countries that are republics?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select count(distinct GovernmentForm) from target.country where continent = 'Africa'")
                .prompt("How many type of governments are in Africa?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select name from target.country where IndepYear > 1950")
                .prompt("What are the names of countries that became independent after 1950?")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select avg(gnp), sum(population) from target.country where GovernmentForm = 'US Territory'")
                .prompt("What is the average GNP and total population in all nations whose government is US territory?")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        ExpVariant q10 = ExpVariant.builder()
//                .queryNum("Q10")
//                .querySql("select t2.language from country as t1, countrylanguage as t2 where t1.code = t2.countrycode on t1.code = t2.countrycode where t1.name = 'Aruba' order by percentage desc limit 1")
//                .prompt("Which language is the most popular in Aruba?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        ExpVariant q11 = ExpVariant.builder()
//                .queryNum("Q11")
//                .querySql("select distinct t1.region from country as t1 join countrylanguage as t2 on t1.code = t2.countrycode where t2.language = 'English' or t2.language = 'Dutch'")
//                .prompt("What are the regions that use English or Dutch?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        ExpVariant q12 = ExpVariant.builder()
//                .queryNum("Q12")
//                .querySql("select t2.language from country as t1 join countrylanguage as t2 on t1.code = t2.countrycode where t1.headofstate = 'Beatrix' and t2.isofficial = 'T'")
//                .prompt("What is the official language spoken in the country whose head of state is Beatrix?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        ExpVariant q13 = ExpVariant.builder()
//                .queryNum("Q13")
//                .querySql("select count(distinct t1.continent) from country as t1 join countrylanguage as t2 on t1.code = t2.countrycode where t2.language = 'Chinese'")
//                .prompt("What is the number of distinct continents where Chinese is spoken?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        ExpVariant q14 = ExpVariant.builder()
//                .queryNum("Q14")
//                .querySql("select region from country as t1 join city as t2 on t1.code = t2.countrycode where t2.name = 'Kabul'")
//                .prompt("Which region is the city Kabul located in?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

//        ExpVariant q15 = ExpVariant.builder()
//                .queryNum("Q15")
//                .querySql("select count(*) from country as t1 join countrylanguage as t2 on t1.code = t2.countrycode where t1.name = 'Afghanistan' and isofficial = 'T'")
//                .prompt("How many official languages does Afghanistan have?")
//                .optimizers(multipleConditionsOptimizers)
//                .build();

        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
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
            testRunner.execute("/world_1_data/world1-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/world_1_data/world1-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
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

}
