package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.llm.algebra.LLMScan;
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
        ExpVariant q1 = ExpVariant.builder()
                .queryNum("Q1")
                .querySql("select t.player_name, t.player_surname from wimbledon_champions t where t.tournament_year = 2023 and t.gender = 'male'")
                .prompt("List the player name and player surname of the male 2023 Wimbledon champion.")
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
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/demo-tennis/demo-tennis-togetherai-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    @Test
    public void testRunContentRetriever() {
        ExpVariant qC = ExpVariant.builder()
                .queryNum("QC")
                .querySql("select t.player_name, t.player_surname from wimbledon_champions t where t.tournament_year = 2025 and t.gender = 'male'")
                .prompt("List the player name and player surname of the male 2025 Wimbledon champion.")
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
