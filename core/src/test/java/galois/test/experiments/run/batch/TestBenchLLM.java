package galois.test.experiments.run.batch;

import galois.llm.query.local.LocalNLQueryExecutor;
import galois.llm.query.local.LocalSQLQueryExecutor;
import galois.llm.query.local.LocalTableQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3CSVTableQueryExecutor;
import galois.optimizer.IOptimizer;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class TestBenchLLM {

    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "bird-results.txt";
    private static final String QUERIES_PATH = "src/test/resources/llm-bench/dataset.xlsx";
    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

//    private String executorModel =  "llama3";
//    private String name = Configuration.getInstance().getTogetheraiModel();

//    private String executorModel = "gpt";
//    private String name = Configuration.getInstance().getOpenaiModelName();

    private String executorModel = "local";
    private String name = Configuration.getInstance().getLocalModelName();

    private List<ExpVariant> variants;
    private Map<String, VariantConfig> variantConfigs;

    public TestBenchLLM() {
        initVariants();
    }

    //    private final List<String> SELECTION = List.of("bird_31", "galois_32", "galois_34", "galois_36", "galois_37", "galois_39", "galois_40", "qatch_10", "qatch_109", "qatch_114", "qatch_115", "qatch_117", "qatch_118", "qatch_12", "qatch_120", "qatch_122", "qatch_128", "qatch_134", "qatch_136", "qatch_138", "qatch_140", "qatch_142", "qatch_159", "qatch_160", "qatch_161", "qatch_163", "qatch_172", "qatch_175", "qatch_178", "qatch_180", "qatch_182", "qatch_190", "qatch_194", "qatch_196", "qatch_199", "qatch_202", "qatch_204", "qatch_206", "qatch_255", "qatch_260", "qatch_269", "qatch_270", "qatch_278", "qatch_279", "qatch_280", "qatch_285", "qatch_288", "qatch_289", "qatch_291", "qatch_294", "qatch_299", "qatch_300", "qatch_304", "qatch_310", "qatch_311", "qatch_315", "qatch_317", "qatch_319", "qatch_321", "qatch_324", "qatch_328", "qatch_330", "qatch_338", "qatch_35", "qatch_410", "qatch_412", "qatch_413", "qatch_415", "qatch_417", "qatch_420", "qatch_422", "qatch_444", "qatch_45", "qatch_455", "qatch_456", "qatch_457", "qatch_459", "qatch_460", "qatch_461", "qatch_462", "qatch_463", "qatch_466", "qatch_467", "qatch_468", "qatch_469", "qatch_470", "qatch_483", "qatch_484", "qatch_489", "qatch_60", "qatch_67", "qatch_68", "qatch_71", "qatch_74", "qatch_76", "qatch_79", "qatch_82", "qatch_88", "qatch_92", "qatch_94");
//    private final List<String> SELECTION = List.of("qatch_7", "bird_64");
//    private final List<String> SELECTION = List.of("bird_1");
//    private final List<String> SELECTION = List.of("bird_6","bird_1","galois_45","galois_40","galois_39","galois_37","bird_19","galois_3","qatch_402","qatch_413","qatch_423","qatch_422","qatch_420","qatch_412","qatch_410","qatch_446","qatch_400","qatch_417","qatch_415","qatch_408","qatch_405","qatch_208","qatch_332","qatch_300","qatch_103","qatch_330","bird_3","bird_25","bird_31","bird_11","bird_8","qatch_78","spider1_2","qatch_66","bird_23","qatch_10","qatch_45","spider1_51","bird_53","qatch_75","qatch_41","qatch_46","qatch_12","qatch_60","qatch_57","qatch_80","qatch_70","qatch_72","qatch_52","qatch_58","qatch_77","qatch_76","qatch_74","qatch_71","qatch_22","qatch_11","qatch_17","qatch_14","qatch_61","qatch_68","qatch_43","qatch_63","qatch_69","qatch_13","galois_32","galois_30","galois_36","galois_33","galois_19","galois_29","galois_16","bird_69","bird_70","bird_71","qatch_467","qatch_458","qatch_465","qatch_464","qatch_461","qatch_456","qatch_457","qatch_466","qatch_463","qatch_462","qatch_460","qatch_455","qatch_450","qatch_336","qatch_335","qatch_342","qatch_337","qatch_334","qatch_496","qatch_354","qatch_375","qatch_234","qatch_137","qatch_495","qatch_470","qatch_377");
    private final List<String> SELECTION = List.of("qatch_234", "qatch_300", "qatch_12", "qatch_10", "qatch_402", "galois_36", "galois_33", "bird_69", "qatch_467", "qatch_103", "qatch_461", "galois_29", "qatch_208", "qatch_58", "qatch_455", "qatch_330");

    @Test
    public void testBench() {
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(name);

        log.info("{} variants!", variants.size());
        int i = 1;

        for (ExpVariant variant : this.variants) {
            VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
            String dbID = vc.db_id;
            String dataset = vc.dataset;

            log.info("Variant {} / {}", i, variants.size());
            i += 1;

            // Uncomment to filter queries based on the SELECTION variable
//            if (!SELECTION.contains(variant.getQueryNum())) continue;

            // Uncomment to enable external knowledge generation (table values are injected in the prompt)
//            ExternalKnowledgeGenerator.getInstance().setGenerate(true);

            testRunner.execute("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", "TABLE-ALL-CONDITIONS", variant, metrics, results, allConditionPushdownWithFilter);

//            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, null);
//            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-csv-table-experiment.json", "CSV", variant, metrics, results, allConditionPushdownWithFilter);

            // Execution which saves the output of the LLM judge inside the database
//            LLMJudgeDBLogger.getInstance().setEnabled(true);
//            LLMJudgeDBLogger.getInstance().setCurrentDataset(dbID);
//            LLMJudgeDBLogger.getInstance().setCurrentDB(dataset);
//            LLMJudgeDBLogger.getInstance().setCurrentQuery(vc.variant.getQuerySql());
//            testRunner.executeSingle("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, allConditionPushdownWithFilter);
//            LLMJudgeDBLogger.getInstance().setEnabled(false);

            exportExcel.export(fileName, dataset, metrics, results);
        }
    }

    @Test
    public void testCSVQuery() throws IOException {
        for (ExpVariant variant : this.variants) {
            VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
            String dbID = vc.db_id;
            String dataset = vc.dataset;

            Experiment experiment = ExperimentParser.loadAndParseJSON("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-csv-table-experiment.json");
            assertEquals(TogetheraiLlama3CSVTableQueryExecutor.class, experiment.getOperatorsConfiguration().getScan().getQueryExecutor().getClass());
        }
    }

    @Test
    public void testLocalQueries() throws IOException {
        for (ExpVariant variant : this.variants) {
            VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
            String dbID = vc.db_id;
            String dataset = vc.dataset;

            Experiment nl = ExperimentParser.loadAndParseJSON("/llm-bench/" + dataset + "/" + dbID + "-local" + "-nl-experiment.json");
            assertEquals(LocalNLQueryExecutor.class, nl.getOperatorsConfiguration().getScan().getQueryExecutor().getClass());
            Experiment sql = ExperimentParser.loadAndParseJSON("/llm-bench/" + dataset + "/" + dbID + "-local" + "-sql-experiment.json");
            assertEquals(LocalSQLQueryExecutor.class, sql.getOperatorsConfiguration().getScan().getQueryExecutor().getClass());
            Experiment table = ExperimentParser.loadAndParseJSON("/llm-bench/" + dataset + "/" + dbID + "-local" + "-table-experiment.json");
            assertEquals(LocalTableQueryExecutor.class, table.getOperatorsConfiguration().getScan().getQueryExecutor().getClass());
        }
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
    public void exportConfidences() {
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
