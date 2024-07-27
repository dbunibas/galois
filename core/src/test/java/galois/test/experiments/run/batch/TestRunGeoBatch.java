package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
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
public class TestRunGeoBatch {

    private static final String EXP_NAME = "CONTINENTS";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "geo-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunGeoBatch() {
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
                .querySql("SELECT state_name, population, area FROM target.usa_state")
                .prompt("List the state name, population and area from USA states")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT us.state_name, us.capital, us.area FROM target.usa_state us")
                .prompt("List the state name, capital and area from USA states")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT state_name, population, area FROM target.usa_state where capital = 'Frankfort'")
                .prompt("List the state name, population and area from USA states where the capital is Frankfort")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT us.state_name, us.population, us.capital FROM target.usa_state us where us.population > 5000000")
                .prompt("List the state name, population and capital from USA states where the population is greater than 5000000")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT us.state_name, us.capital FROM public.usa_state us where us.population > 5000000 AND us.density < 1000")
                .prompt("List the state name and capital from USA states where the population is greater than 5000000 and the density is lower than 1000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT us.state_name, us.capital, us.density, us.population FROM target.usa_state us where us.population > 5000000 AND us.density < 1000 AND us.area < 50000")
                .prompt("List the state name, capital, density and population from USA states where the population is greater than 5000000, the density is lower than 1000 and the area is lower than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select us.state_name, us.capital from usa_state us where us.population > 3000000 AND us.area > 50000 order by us.capital")
                .prompt("List the state name and capital ordered by capital from USA states where the population is greater than 3000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select us.state_name, us.capital, us.population from usa_state us where us.population > 3000000 AND us.population < 8000000 AND us.area > 50000 order by us.population")
                .prompt("List the state name capital and population ordered by population from USA states where the population is greater than 3000000 and lower than 8000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select us.state_name, us.capital, us.population, us.area from usa_state us where us.population = 4700000 AND area=56153")
                .prompt("List the state name, the capital, the popoulation and the area from USA states where the population is 4700000 and the are is 56153")
                .optimizers(multipleConditionsOptimizers)
                .build();

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
            testRunner.execute("/geo_data/geo-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/geo_data/geo-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/geo_data/geo-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/geo_data/geo-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/geo_data/geo-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
}
