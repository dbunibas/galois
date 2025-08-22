package galois.test.experiments.run.batch;

import galois.Constants;
import galois.optimizer.IOptimizer;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestBenchLLM {

    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "bird-results.txt";
    private static final String QUERIES_PATH = "src/test/resources/llm-bench/dataset.xlsx";
    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();
//    private String executorModel = "llama3";
//    private String name = Constants.TOGETHERAI_MODEL;
    private String executorModel = "gpt";
    private String name = Configuration.getInstance().getOpenaiModelName();

    private List<ExpVariant> variants;
    private Map<String, VariantConfig> variantConfigs;

    public TestBenchLLM() {
        initVariants();
    }

    @Test
    public void testBench() {
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(name);
        for (ExpVariant variant : this.variants) {
            VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
            String dbID = vc.db_id;
            String dataset = vc.dataset;
            testRunner.execute("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, allConditionPushdownWithFilter);
            exportExcel.export(fileName, dataset, metrics, results);
        }
        log.info("Results\n{}", printMap(results));

    }

    private void initVariants() {
        List<String> singleConditionOptimizers = List.of(
                //                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        variants = new ArrayList<>();
        variantConfigs = new HashMap<>();
        System.out.println("Open: " + QUERIES_PATH);
        try (FileInputStream fis = new FileInputStream(new File(QUERIES_PATH)); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("queries");
            if (sheet == null) {
                System.out.println("Sheet 'queries' not found!");
                return;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell q_idCell = row.getCell(0);
                    Cell dbIdCell = row.getCell(2);
                    Cell queryCell = row.getCell(3);
                    Cell questionCell = row.getCell(4);
                    Cell datasetCell = row.getCell(1);

                    String dbId = (dbIdCell != null) ? dbIdCell.toString() : "";
                    String query = (queryCell != null) ? queryCell.toString() : "";
                    String question = (questionCell != null) ? questionCell.toString() : "";
                    String dataset = (datasetCell != null) ? datasetCell.toString() : "";
                    String queryNum = (q_idCell != null) ? q_idCell.toString() : "";

                    if (dbId == null || dbId.isEmpty()) {
                        break;
                    }
                    ExpVariant ev = ExpVariant.builder()
                            .queryNum(queryNum)
                            .querySql(query)
                            .prompt(question)
                            .optimizers(singleConditionOptimizers)
                            .build();
                    VariantConfig vc = new VariantConfig(ev, dataset, dbId);
                    variants.add(ev);
                    variantConfigs.put(ev.getQueryNum(), vc);
                }
            }
        } catch (IOException e) {
            System.out.println("Error in loading queries - File: " + QUERIES_PATH);
            e.printStackTrace();
        }
    }

    @Test
    public void exportConfidences(){
        StringBuilder result = new StringBuilder("Query ID\tTABLE Conf\tSQL Conf\tNL Conf\n");
        for (ExpVariant variant : this.variants) {
            result.append(variant.getQueryNum()).append("\t");
            result.append(testRunner.getConfidenceValue("TABLE", variant)).append("\t");
            result.append(testRunner.getConfidenceValue("SQL", variant)).append("\t");
            result.append(testRunner.getConfidenceValue("NL", variant)).append("\n");
        }
        log.info("### Confidences ##\n{}", result);
        StringSelection selection = new StringSelection(result.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private class VariantConfig {

        private ExpVariant variant;
        private String dataset;
        private String db_id;

        public VariantConfig(ExpVariant variant, String dataset, String db_id) {
            this.variant = variant;
            this.dataset = dataset;
            this.db_id = db_id;
        }

    }

}
