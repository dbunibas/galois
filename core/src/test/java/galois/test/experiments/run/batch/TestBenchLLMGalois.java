package galois.test.experiments.run.batch;

import galois.optimizer.IOptimizer;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import speedy.model.database.Tuple;
import speedy.persistence.Types;
import static speedy.persistence.Types.DOUBLE_PRECISION;
import static speedy.persistence.Types.INTEGER;
import static speedy.persistence.Types.LONG;
import static speedy.persistence.Types.REAL;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestBenchLLMGalois {

    private static final String EXP_NAME = "galois";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "recipes-results.txt";
    public static final String QUERIES_PATH = "src/test/resources/llm-bench/galois/queries.xlsx";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();
    private Map<String, List<ExpVariant>> variants;
    private String executorModel = "llama3"; // useful only for GPT

    public TestBenchLLMGalois() {
        List<String> multipleConditionsOptimizers = List.of(
                "SingleConditionsOptimizerFactory",
                "SingleConditionsOptimizerFactory-WithFilter",
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        this.variants = loadQuestions();
    }

    @Test
    public void testLoad() {
        for (String dbID : variants.keySet()) {
            System.out.println("DB:" + dbID);
            List<ExpVariant> variantsForDB = variants.get(dbID);

        }
    }

    @Test
    public void testExpected() {
        List<String> expTabs = new ArrayList<>();
        for (String dbID : variants.keySet()) {
            System.out.println("DB:" + dbID);
            if (!dbID.equalsIgnoreCase("world1")) continue;
            List<ExpVariant> variantsForDB = variants.get(dbID);
            for (ExpVariant variant : variantsForDB) {
                List<Tuple> expected = testRunner.computeExpected("/llm-bench/" + EXP_NAME + "/" + dbID + "-" + executorModel + "-nl-experiment.json", variant);
                int cells = countCells(expected);
                int attrs = countAttrs(expected);
                int attrNumerical = countNumerical(expected);
                int attrCategorical = countCategorical(expected, attrNumerical);

//                String expTab = variant.getQueryNum() + "\t" + expected.size() + "\t" + cells + "\t" + attrs;
                String expTab = variant.getQueryNum() + "\t" + attrNumerical + "\t" + attrCategorical;

                expTabs.add(expTab);
            }
        }
        System.out.println("*********************************");
        for (String expTab : expTabs) {
            System.out.println(expTab);
        }
    }

    @Test
    public void testRunBatch() {
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (String dbID : variants.keySet()) {
            System.out.println("DB:" + dbID);
//            if (!dbID.equals("academic")) continue;
            List<ExpVariant> variantsForDB = variants.get(dbID);
            for (ExpVariant variant : variantsForDB) {
//                if (!variant.getQueryNum().equalsIgnoreCase("Q2")) continue;
                testRunner.execute("/llm-bench/" + EXP_NAME + "/" + dbID + "-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
                testRunner.execute("/llm-bench/" + EXP_NAME + "/" + dbID + "-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
                testRunner.executeSingle("/llm-bench/" + EXP_NAME + "/" + dbID + "-" + executorModel + "-table-experiment.json", "TABLE", variant, metrics, results, allConditionPushdownWithFilter);
                exportExcel.export(fileName, EXP_NAME, metrics, results);
            }
        }
        log.info("Results\n{}", printMap(results));
    }

    private Map<String, List<ExpVariant>> loadQuestions() {
        List<String> singleConditionOptimizers = List.of(
                //                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );

        System.out.println("Open: " + QUERIES_PATH);
        try (FileInputStream fis = new FileInputStream(new File(QUERIES_PATH)); Workbook workbook = new XSSFWorkbook(fis)) {
            Map<String, List<ExpVariant>> queries = new HashMap<>();
            Sheet sheet = workbook.getSheet("queries");
            if (sheet == null) {
                System.out.println("Sheet 'queries' not found!");
                return queries;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell dbIdCell = row.getCell(0);
                    Cell queryCell = row.getCell(1);
                    Cell questionCell = row.getCell(2);

                    String dbId = (dbIdCell != null) ? dbIdCell.toString() : "";
                    String query = (queryCell != null) ? queryCell.toString() : "";
                    String question = (questionCell != null) ? questionCell.toString() : "";
                    if (dbId == null || dbId.isEmpty()) {
                        break;
                    }
                    ExpVariant ev = ExpVariant.builder()
                            .queryNum("Q" + i)
                            .querySql(query)
                            .prompt(question)
                            .optimizers(singleConditionOptimizers)
                            .build();
                    List<ExpVariant> evs = queries.get(dbId);
                    if (evs == null) {
                        evs = new ArrayList<>();
                        queries.put(dbId, evs);
                    }
                    evs.add(ev);
                }
            }
            return queries;
        } catch (IOException e) {
            System.out.println("Error in loading queries - File: " + QUERIES_PATH);
            e.printStackTrace();
            return null;
        }
    }

    private int countCells(List<Tuple> tuples) {
        int count = 0;
        for (Tuple tuple : tuples) {
            int cellsWithoutOID = tuple.getCells().size() - 1;
            if (cellsWithoutOID > 0) {
                count += cellsWithoutOID;
            }
        }
        return count;
    }

    private int countAttrs(List<Tuple> tuples) {
        int count = 0;
        if (tuples.size() > 0) {
            Tuple tuple = tuples.get(0);
            int cellsWithoutOID = tuple.getCells().size() - 1;
            if (cellsWithoutOID > 0) {
                return cellsWithoutOID;
            }

        }
        return count;
    }

    private int countNumerical(List<Tuple> tuples) {
        int count = 0;
        Tuple firstTuple = tuples.get(0);
        String[] numericalTypes = {INTEGER, LONG, REAL, DOUBLE_PRECISION};
        for (speedy.model.database.Cell cell : firstTuple.getCells()) {
            if (cell.isOID()) {
                continue;
            }
            for (String numericalType : numericalTypes) {
                if (Types.checkType(numericalType, cell.getValue().getPrimitiveValue().toString())) {
                    count++;
                    System.out.println("Count numerical");
                    break;
                }
            }
        }
        return count;
    }

    private int countCategorical(List<Tuple> tuples, int numerical) {
        Tuple firstTuple = tuples.get(0);
        return firstTuple.getCells().size() - 1 - numerical;
    }

}
