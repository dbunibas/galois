package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.llm.algebra.LLMScan;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.utils.ExternalKnowledgeGenerator;
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
public class TestExternalBatch {
    private static final String EXP_NAME = "External-Knowledge";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "external-knowledge-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "togetherai";

    public TestExternalBatch() {
        ExpVariant q0 = ExpVariant.builder()
                .queryNum("Q0")
                .querySql("SELECT DISTINCT p.name, p.party FROM target.world_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name and party of Venezuela presidents.")
                .build();

        variants = List.of(q0);
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
    public void testRunBatchTogetherAI() {
        executeExperiments(Constants.PROVIDER_TOGETHERAI);
    }

    @Test
    public void testRunBatchOpenAI() {
        executeExperiments(Constants.PROVIDER_OPENAI);
    }

    private void executeExperiments(String provider) {
        // Enable external knowledge generation
        ExternalKnowledgeGenerator.getInstance().setGenerate(true);

        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME + "-" + provider);

        for (ExpVariant variant : variants) {
            String configPathNl = "/presidents/presidents-" + provider + "-nl-experiment.json";
            String configPathSql = "/presidents/presidents-" + provider + "-sql-experiment.json";
            String configPathTable = "/presidents/presidents-" + provider + "-table-experiment.json";

            testRunner.execute(configPathNl, "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute(configPathSql, "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute(configPathTable, "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);

            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }

        log.info("Results\n{}", printMap(results));
    }
}
