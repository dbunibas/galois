package galois.test.experiments.run.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galois.sqlparser.SQLQueryParser;
import galois.Constants;
import galois.llm.algebra.LLMScan;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.utils.QueryUtils;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.optimizer.QueryPlan;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.test.utils.TestUtils;
import galois.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Key;
import speedy.model.database.Tuple;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;
import speedy.persistence.relational.QueryManager;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
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
    private String executorModel = "llama3";

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
                .querySql("select ceo from fortune_2024 f where  is_femaleceo = 'Yes' and f.private_or_public = 'Private'")
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
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where company = 'Amazon'")
                .prompt("List the company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees for the company named 'Amazon' from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where headquarterscity = 'Santa Clara' and rank < 70")
                .prompt("List company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees, for companies headquartered in 'Santa Clara' with a rank below 70 from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

//        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10);
        variants = List.of( q6);
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
    public void testComparePalimpzest() throws IOException {
        String pzResultPath = "/Users/donatello/Projects/research/[Related Tools]/palimpzest/exp-results";
//        String pzVariant = "Maximum Quality";
        String pzVariant = "MaxQuality@FixedCost";
//        String pzVariant = "Minimum Cost";
        MainMemoryDB pzResults = new DAOMainMemoryDatabase().loadCSVDatabase(pzResultPath + File.separator + pzVariant, ';', null, false, true);
        StringBuilder resultString = new StringBuilder();
        for (ExpVariant variant : variants) {
            String pzVariantResultPath = pzResultPath + File.separator + pzVariant + File.separator + "Galois-Fortune-RAG-" + variant.getQueryNum();
            log.info("Loading Palimpzest result from path {}", pzVariantResultPath);
            String pzVariantCSVResultPath = pzVariantResultPath + ".csv";
            String pzVariantDetailResultPath = pzVariantResultPath + ".json";
            Map<String, Object> pzVariantDetails = new ObjectMapper().readValue(new File(pzVariantDetailResultPath), new TypeReference<>() {
            });
            Number totalUsedTokens = (Number) pzVariantDetails.get("total_used_tokens");

            //QUALITY
            Experiment experiment = ExperimentParser.loadAndParseJSON("/rag-fortune/fortune2024-llama3-pz-experiment.json"); //Used only for load expected result
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            DBMSDB dbmsDatabase = experiment.createDatabaseForExpected();
            String queryToExecute = experiment.getQuery().getSql().replace("target.", "public.");
            log.debug("Query for results:\n{}", queryToExecute);
            ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
            ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
            List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
            expectedITerator.close();
            log.info("Expected size: {}", expectedResults.size());
            ITupleIterator actual = pzResults.getTable("Galois-Fortune-RAG-" + variant.getQueryNum()).getTupleIterator();
            ExperimentResults experimentResults = experiment.toExperimentResults(actual, expectedResults, "PZ-" + pzVariant);
            log.warn("{}", experimentResults.getScores());
            double quality = (
                    (experimentResults.getScores().get(5) != null ? experimentResults.getScores().get(5)  : 0)  + //CellSimilarityF1Score
                            (experimentResults.getScores().get(6) != null ? experimentResults.getScores().get(6)  : 0) + //TupleCardinality
                            (experimentResults.getScores().get(8) != null ? experimentResults.getScores().get(8)  : 0) //TupleSimilarityConstraint
            ) / 3.0;
            log.info("\n******** \n* PZ {} - Query {}\n* Total Used Tokens: {}\n* Quality: {}\n********", pzVariant, variant.getQueryNum(), totalUsedTokens, experimentResults.toDebugString());
            resultString.append((quality + "").replace(".", ",") + "\t" + totalUsedTokens + "\n");
        }
        log.info("******** PZ {} ********\n{}", pzVariant, resultString);
    }


    @Test
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
//            testRunner.execute("/rag-fortune/fortune2024-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-fortune/fortune2024-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-fortune/fortune2024-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-fortune/fortune2024-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    @Test
    public void testRunBatch2() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            testRunner.execute("/rag-fortune/fortune2024-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-fortune/fortune2024-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            IOptimizer allCondition = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
            String configPathTable = "/rag-fortune/fortune2024-" + executorModel + "-table-experiment.json";
            String configPathKey = "/rag-fortune/fortune2024-" + executorModel + "-key-scan-experiment.json";
            testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allCondition);
            testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allCondition);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }
    
    @Test
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = true;
        boolean execute = false;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            if (execute) testRunner.execute("/rag-fortune/fortune2024-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/rag-fortune/fortune2024-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/rag-fortune/fortune2024-" + executorModel + "-table-experiment.json";
            String configPathKey = "/rag-fortune/fortune2024-" + executorModel + "-key-scan-experiment.json";
            QueryPlan planEstimation = testRunner.planEstimation(configPathTable, variant); // it doesn't matter
            log.info("Plan Estimated: {}", planEstimation);
            String pushDownStrategy = planEstimation.computePushdown();
            Double confidenceKeys = planEstimation.getConfidenceKeys();
            Integer indexPushDown = planEstimation.getIndexPushDown();
            IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer"); //remove algebra false
            IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
            IOptimizer singleConditionPushDownRemoveAlgebraTree = null;
            IOptimizer singleConditionPushDown = null;
            if (indexPushDown != null) {
                singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexPushDown, true);
                singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexPushDown, false);
            }
            IOptimizer optimizer = null;
            if (pushDownStrategy.equals(QueryPlan.PUSHDOWN_ALL_CONDITION)) {
//                optimizer = allConditionPushdown;
                optimizer = allConditionPushdownWithFilter;
            }
            if (pushDownStrategy.startsWith(QueryPlan.PUSHDOWN_SINGLE_CONDITION)) {
//                optimizer = singleConditionPushDown;
                optimizer = singleConditionPushDownRemoveAlgebraTree;
            }
            if (executeAllPlans) {
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                IOptimizer allCondition = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allCondition);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allCondition);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                }
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
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
        Map<String, ExperimentResults> experimentResultsMap = testRunner.executeSingle(configPath, type, variant, metrics, results, allConditionPushdownWithFilter);
        ExperimentResults experimentResults = experimentResultsMap.values().iterator().next();
        double quality = (
                (experimentResults.getScores().get(5) != null ? experimentResults.getScores().get(5)  : 0)  + //CellSimilarityF1Score
                        (experimentResults.getScores().get(6) != null ? experimentResults.getScores().get(6)  : 0) + //TupleCardinality
                        (experimentResults.getScores().get(8) != null ? experimentResults.getScores().get(8)  : 0) //TupleSimilarityConstraint
        ) / 3.0;
        log.info("{}: {}", experimentResults.getName(), quality);
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
