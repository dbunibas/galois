package galois.test.experiments.run.batch;

import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
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
import speedy.SpeedyConstants;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Tuple;
import speedy.model.database.TupleOID;
import static speedy.SpeedyConstants.OID;
import speedy.model.database.AttributeRef;
import speedy.model.database.ConstantValue;
import speedy.model.database.IValue;
import speedy.model.database.TableAlias;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestBenchLLMSimpleQA {

    private static final String EXP_NAME = "simpleqa";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "recipes-results.txt";
    public static final String QUERIES_PATH = "src/test/resources/llm-bench/simpleqa/queries.xlsx";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();
    private List<ExpVariant> variants;
    private List<String> attributeName = new ArrayList<>();
    private List<String> cellValues = new ArrayList<>();
    private Map<String, String> attributeForVariant = new HashMap<>();
    private Map<String, String> valueForVariant = new HashMap<>();
    private String executorModel = "llama3"; // useful only for GPT

    public TestBenchLLMSimpleQA() {
        List<String> multipleConditionsOptimizers = List.of(
                "SingleConditionsOptimizerFactory",
                "SingleConditionsOptimizerFactory-WithFilter",
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        this.variants = loadQuestions();
        initResults();
    }

    @Test
    public void testLoad() {
        System.out.println(this.variants.size());
    }

    @Test
    public void testExpected() {
        IDatabase database = null;
        try {
            Experiment experiment = ExperimentParser.loadAndParseJSON("/llm-bench/" + EXP_NAME + "/" + EXP_NAME + "-" + executorModel + "-nl-experiment.json");
            database = experiment.getQuery().getDatabase();
        } catch (IOException iOException) {
            System.out.println("*** ERROR");
            return;
        }
        List<String> expTabs = new ArrayList<>();
        System.out.println("Variants: " + this.variants.size());
        for (ExpVariant variant : this.variants) {
            List<Tuple> expected = this.computeExpected(variant, database);
            int cells = countCells(expected);
            String expTab = variant.getQueryNum() + "\t" + expected.size() + "\t" + cells;
            expTabs.add(expTab);
        }
        System.out.println("*********************************");
        for (String expTab : expTabs) {
            System.out.println(expTab);
        }
    }

    private List<Tuple> computeExpected(ExpVariant variant, IDatabase database) {
        long oid = 1;
        String tableName = this.attributeForVariant.get(variant.getQueryNum());
        ITable table = database.getTable(tableName);
        String cellValue = this.valueForVariant.get(variant.getQueryNum());
        TupleOID tupleOID = new TupleOID(oid);
        Tuple tuple = new Tuple(tupleOID);
        speedy.model.database.Cell oidCell = new speedy.model.database.Cell(tupleOID, new AttributeRef(tableName, SpeedyConstants.OID), new ConstantValue(tupleOID));
        tuple.addCell(oidCell);
        for (Attribute attribute : table.getAttributes()) {
            if (!attribute.getName().equalsIgnoreCase(OID)) {
                AttributeRef aRef = new AttributeRef(tableName, attribute.getName());
//                String type = attribute.getType();
//                Object typedValue = Types.getTypedValue(type, cellValue);
                IValue value = new ConstantValue(cellValue);
                speedy.model.database.Cell cell = new speedy.model.database.Cell(tupleOID, aRef, value);
//                System.out.println("CELL: " + cell);
                tuple.addCell(cell);
            }
        }
        List<Tuple> tuples = new ArrayList<>();
        tuples.add(tuple);
        return tuples;
    }

    @Test
    public void testRunBatch() {
        IDatabase database = null;
        String configFileName = "/llm-bench/" + EXP_NAME + "/" + EXP_NAME + "-" + executorModel + "-nl-experiment.json";
        try {
            Experiment experiment = ExperimentParser.loadAndParseJSON(configFileName);
            database = experiment.getQuery().getDatabase();
        } catch (IOException iOException) {
            System.out.println("*** ERROR");
            return;
        }
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : this.variants) {
            List<Tuple> expected = this.computeExpected(variant, database);
            String tableName = this.attributeForVariant.get(variant.getQueryNum());
            TableAlias tableAlias = new TableAlias(tableName);
            testRunner.executeWithExpected(configFileName, "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE, expected, database, tableAlias);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    private List<ExpVariant> loadQuestions() {
        List<String> singleConditionOptimizers = List.of(
                //                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );

        System.out.println("Open: " + QUERIES_PATH);
        List<ExpVariant> variants = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(new File(QUERIES_PATH)); Workbook workbook = new XSSFWorkbook(fis)) {
            Map<String, List<ExpVariant>> queries = new HashMap<>();
            Sheet sheet = workbook.getSheet("queries");
            if (sheet == null) {
                System.out.println("Sheet 'queries' not found!");
                return variants;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell questionCell = row.getCell(0);
                    Cell answerTypeCell = row.getCell(1);
                    Cell answerCell = row.getCell(2);

                    String question = (questionCell != null) ? questionCell.toString() : "";
                    String answerType = (answerTypeCell != null) ? answerTypeCell.toString() : "";
                    String answer = (answerCell != null) ? answerCell.toString() : "";
                    this.attributeName.add(answerType);
                    this.cellValues.add(answer);

                    ExpVariant ev = ExpVariant.builder()
                            .queryNum("Q" + i)
                            .querySql(null)
                            .prompt(question)
                            .optimizers(singleConditionOptimizers)
                            .build();
                    variants.add(ev);
                }
            }
            return variants;
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

    private void initResults() {
        for (int i = 0; i < this.variants.size(); i++) {
            String attribute = this.attributeName.get(i);
            String cellValue = this.cellValues.get(i);
            ExpVariant variant = this.variants.get(i);
            this.attributeForVariant.put(variant.getQueryNum(), attribute);
            this.valueForVariant.put(variant.getQueryNum(), cellValue);
        }
    }

}
