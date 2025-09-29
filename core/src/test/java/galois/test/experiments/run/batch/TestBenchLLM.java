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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
//    private static final String QUERIES_PATH = "src/test/resources/llm-bench/limitQuery.xlsx";
    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();
    private String executorModel = "llama3";
    private String name = Constants.TOGETHERAI_MODEL;
//    private String executorModel = "gpt";
//    private String name = Constants.OPEN_AI_CHAT_MODEL_NAME;

    private List<ExpVariant> variants;
    private Map<String, VariantConfig> variantConfigs;
    
    private List<String> queryExecute = List.of(
                "qatch_333", "qatch_334", "qatch_335", "qatch_336", "qatch_337", "qatch_338", "qatch_339", "qatch_340", "qatch_341", "qatch_342", "qatch_343", "qatch_344", "qatch_345", "qatch_346", "qatch_347", "qatch_348", "qatch_349", "qatch_350", "qatch_351", "qatch_352", "qatch_353", "qatch_354", "qatch_355", "qatch_356", "qatch_357", "qatch_358"
        );

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
//            if (!queryExecute.contains(variant.getQueryNum())) continue;
//            if (!variant.getQueryNum().equalsIgnoreCase("qatch_88")) continue;
//            if (!variant.getQueryNum().equalsIgnoreCase("qatch_30")) continue;
//            if (!variant.getQueryNum().equalsIgnoreCase("bird_68")) continue;
//            if (!variant.getQueryNum().equalsIgnoreCase("galois_72")) continue;
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
    
    @Test
    public void testGTInformationPresidents() {
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(name);
        String queryUSA = "SELECT name, start_year, end_year, cardinal_number, party, country FROM target.world_presidents p WHERE p.country='United States'";
        String queryVenezuela = "SELECT name, start_year, end_year, cardinal_number, party, country FROM target.world_presidents p WHERE p.country='Venezuela'";
        String queryNobelPrize = "SELECT nobel_prize_laureate, nobel_prize, nobel_prize_year, nationality, birth_year FROM nobel_prize WHERE nationality='United States'";
//        String queryChecmicalElements = "SELECT atomic_number, symbol, element, period, block, atomic_weight, phase FROM chemical_element";
        String queryChecmicalElements = "SELECT atomic_number, symbol, element FROM chemical_element";
        List<String> singleConditionOptimizers = List.of(
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        ExpVariant variantUSA = new ExpVariant("Q-Usa", queryUSA, "", singleConditionOptimizers);
        ExpVariant variantVenezuela = new ExpVariant("Q-Venezuela", queryVenezuela, "", singleConditionOptimizers);
        ExpVariant variantNobelPrize = new ExpVariant("Q-NobelPrize", queryNobelPrize, "", singleConditionOptimizers);
        ExpVariant variantChemicalElements = new ExpVariant("Q-ChemicalElements", queryChecmicalElements, "", singleConditionOptimizers);
//        String dataset = "galois";
//        String dbID = "presidents";
        String dataset = "qatch";
        String dbID = "nobel_prize";
//        String dbID = "chemical_element";
        List<ExpVariant> variants = new ArrayList<>();
//        variants.add(variantUSA);
//        variants.add(variantVenezuela);
        variants.add(variantNobelPrize);
        for (ExpVariant variant : variants) {
            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-key-experiment.json", "KEY", variant, metrics, results, allConditionPushdownWithFilter);
            exportExcel.export(fileName, dataset, metrics, results);
        }
        log.info("Results\n{}", printMap(results));

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
