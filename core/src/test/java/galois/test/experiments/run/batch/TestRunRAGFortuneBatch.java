package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.llm.algebra.LLMScan;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.utils.QueryUtils;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunRAGFortuneBatch {

    private static final String EXP_NAME = "RAG-Fortune";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "rag-fortune-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunRAGFortuneBatch() {
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
                .querySql("select f.rank, f.company from fortune_2024 f order by f.rank asc limit 10")
                .prompt("List the rank and company names of the top 10 companies according to the Fortune 2024 ranking.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("select company, ceo from fortune_2024 f where f.headquartersstate = 'Oklahoma'")
                .prompt("Give me a list of all companies and their CEOs that are headquartered in Oklahoma, according to the 'fortune_2024' data.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("select company, headquartersstate from fortune_2024 where number_of_employees > 1000000")
                .prompt("List the company names and the states where they are headquartered for any companies in the Fortune 2024 list that have more than one million employees")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select headquarterscity from fortune_2024 f where f.industry = 'Airlines'")
                .prompt("List the names of the cities where the headquarters of all the airline companies in the Fortune 2024 list are located")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select company from fortune_2024 f where f.sector = 'Technology' and founder_is_ceo = true and is_profitable = true")
                .prompt("List the companies from the Fortune 2024 list that are in the Technology sector, are profitable, and have their founder as the current CEO")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("select ceo from fortune_2024 f where  is_femaleceo = true and f.companytype = 'Private'")
                .prompt("List the CEOs from the Fortune 2024 list who are female and lead privately held companies")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select company from fortune_2024 where best_companies_to_work_for = true and industry = 'Airlines'")
                .prompt("List the airline companies from the Fortune 2024 list that are also included in the 'Best Companies to Work For' ranking")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select company, ceo from fortune_2024 f where f.is_profitable = true and f.is_femaleceo = true and f.headquartersstate = 'Texas'")
                .prompt("List the companies in Texas with female CEOs that were profitable in 2024, according to the Fortune 2024 list.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where company = 'Nvidia'")
                .prompt("List the company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees for the company named 'Nvidia' from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where headquarterscity = 'Santa Clara' and rank < 70")
                .prompt("List company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees, for companies headquartered in 'Santa Clara' with a rank below 70 from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        variants = List.of(q2, q3, q4, q5, q6, q7, q8);
        variants = List.of(q10);
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
            testRunner.execute("/rag-fortune/fortune2024-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-fortune/fortune2024-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
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
        String configPath = "/rag-fortune/fortune2024-llama3-table-experiment.json";
        String type = "TABLE";
        int indexSingleCondition = 2;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
//        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, singleConditionPushDownRemoveAlgebraTree);
    }
    
    @Test
    public void testConfidenceEstimatorSchema() {
        for (ExpVariant variant : variants) {
            String configPath = "/rag-fortune/fortune2024-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
            String configPath = "/rag-fortune/fortune2024-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/rag-fortune/fortune2024-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorQuery(configPath, variant);
//            break;
        }
    }

    @Test
    public void testRunExperiment() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/rag-fortune/fortune2024-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String tableExp = "/rag-fortune/fortune2024-llama3-table-experiment.json";
            String keyExp = "/rag-fortune/fortune2024-llama3-key-scan-experiment.json";
//            Double popularity = getPopularity(tableExp, "usa_state", variant);
            Double popularity = testRunner.getPopularity(tableExp, variant);
            if (popularity >= 0.7) {
                log.info("Run KEY-CAN");
//                IOptimizer optimizer = testRunner.getOptimizerBasedOnCardinality(tableExp, variant);
                IOptimizer optimizer = testRunner.getOptimizerBasedOnLLMOptimization(tableExp, variant);
                testRunner.executeSingle(keyExp, "KEY-SCAN", variant, metrics, results, optimizer);
//                testRunner.execute(keyExp, "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            } else {
                log.info("Run TABLE");
//                IOptimizer optimizer = testRunner.getOptimizerBasedOnCardinality(tableExp, variant);
                IOptimizer optimizer = testRunner.getOptimizerBasedOnLLMOptimization(tableExp, variant);
                testRunner.executeSingle(tableExp, "TABLE", variant, metrics, results, optimizer);
//                testRunner.execute(tableExp, "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    private Double getPopularity(String expPath, String tableName, ExpVariant variant) {
        Experiment experiment = null;
        try {
            experiment = ExperimentParser.loadAndParseJSON(expPath);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return -1.0;
        }
        IDatabase database = experiment.getQuery().getDatabase();
        ITable table = database.getTable(tableName);
        List<Key> keys = database.getPrimaryKeys();
        Key key = keys.get(0);
        String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(variant.getQuerySql());
        String whereExpression = parserWhere.getWhereExpression();
        String prompt = "Given the following JSON schema:\n";
        prompt += jsonSchema + "\n";
        prompt += "What is the popularity in your knowledge of " + key.toString() + " of " + tableName;
        if (whereExpression != null && !whereExpression.trim().isEmpty()) {
            prompt += " where " + whereExpression;
        }
        prompt += "?\n";
        prompt += "Return a value between 0 and 1. Where 1 is very popular and 0 is not popular at all.\n"
                + "Respond with JSON only with a numerical property with name \"popularity\".";
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_8B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        String cleanResponse = Mapper.toCleanJsonObject(response);
        Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        Double popularity = (Double) parsedResponse.getOrDefault("popularity", -1.0);
        return popularity;
    }

}
