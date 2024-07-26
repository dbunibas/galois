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
public class TestRunVenezuelaPresidentsBatch {
    private static final String EXP_NAME = "VENEZUELA_PRESIDENTS";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "venezuela-presidents-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunVenezuelaPresidentsBatch() {
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
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name and party of Venezuela presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='Venezuela' and p.party='Liberal'")
                .prompt("List the name and party of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) as party from target.international_presidents p WHERE p.country='Venezuela' and p.party='Liberal'")
                .prompt("Count the number of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='Venezuela' and p.party='Liberal'")
                .prompt("List the name of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='Venezuela' and p.party='Liberal' and p.start_year > 1858")
                .prompt("List the name of Venezuela presidents after 1858 where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party from target.international_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name, the start year, the end year, the number of president and the party of Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num from target.international_presidents p WHERE p.country='Venezuela' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) from target.international_presidents p where p.country='Venezuela' and p.start_year >= 1990  and p.start_year < 2000")
                .prompt("count Venezuela presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name from target.international_presidents p where p.country='Venezuela' and p.party = 'Military' order by p.end_year desc limit 1")
                .prompt("List the name of the last Venezuela president where party is Military")
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
        for (ExpVariant variant : variants) {
            testRunner.execute("/presidents/presidents-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/presidents/presidents-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
        }
        log.info("Results\n{}", printMap(results));
        String fileName = exportExcel.getFileName(EXP_NAME);
        exportExcel.export(fileName, EXP_NAME, metrics, results);
    }
}
