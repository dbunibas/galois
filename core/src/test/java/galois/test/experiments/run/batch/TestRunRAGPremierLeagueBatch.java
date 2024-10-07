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
public class TestRunRAGPremierLeagueBatch {

    private static final String EXP_NAME = "RAG-PremierLeague";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "rag-premierleague-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunRAGPremierLeagueBatch() {
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
                .querySql("select m.opponent_team, m.match_date_year, m.match_date_month, m.match_date_day from premier_league_2024_2025_arsenal_matches m")
                .prompt("List the date (year, month, day) and opponent for each of Arsenal's 2024-25 Premier League season matches")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("select count(*) from premier_league_2024_2025_arsenal_matches m where m.match_date_month = 8")
                .prompt("Count the number of Arsenal matches in the Premier League for the month of August during the 2024-2025 season")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("select player_name from premier_league_2024_2025_player_of_the_match potm")
                .prompt("List the names of the players who were awarded 'Player of the Match' during the 2024-2025 Premier League season")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select player_name, player_team from premier_league_2024_2025_player_of_the_match potm where potm.player_team = 'Manchester United'")
                .prompt("List the names of the players who were awarded 'Player of the Match' while playing for Manchester United during the 2024-2025 Premier League season")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select home_team,away_team,home_goals,away_goals from premier_league_2024_2025_match_result")
                .prompt("Retrieve the match details from the 2024-2025 Premier League season, including the home team, away team, and the number of goals scored by both teams")
                .optimizers(singleConditionOptimizers)
                .build();

//        variants = List.of(q1, q2, q3, q4, q5);
        variants = List.of(q2);
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
//            testRunner.execute("/rag-premierleague/pl2425-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-premierleague/pl2425-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-premierleague/pl2425-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-premierleague/pl2425-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/rag-premierleague/pl2425-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        log.info("Results\n{}", printMap(results));
    }

    @Test
    public void testSingle() {
        // TO DEBUG single experiment
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        ExpVariant variant = variants.get(4);
        String configPath = "/rag-premierleague/pl2425-llama3-table-experiment.json";
        String type = "TABLE";
        int indexSingleCondition = 0;
        IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        IOptimizer singleConditionPushDownRemoveAlgebraTree = new IndexedConditionPushdownOptimizer(indexSingleCondition, true);
        IOptimizer singleConditionPushDown = new IndexedConditionPushdownOptimizer(indexSingleCondition, false);
        IOptimizer nullOptimizer = null; // to execute unomptimize experiments
        testRunner.executeSingle(configPath, type, variant, metrics, results, singleConditionPushDown);
    }
    
    @Test
    public void testConfidenceEstimatorSchema() {
        for (ExpVariant variant : variants) {
            String configPath = "/rag-premierleague/pl2425-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
            String configPath = "/rag-premierleague/pl2425-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/rag-premierleague/pl2425-llama3-table-experiment.json";
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
            testRunner.execute("/rag-premierleague/pl2425-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/rag-premierleague/pl2425-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String tableExp = "/rag-premierleague/pl2425-llama3-table-experiment.json";
            String keyExp = "/rag-premierleague/pl2425-llama3-key-scan-experiment.json";
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
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_8B);
        String response = model.generate(prompt);
        String cleanResponse = Mapper.toCleanJsonObject(response);
        Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        Double popularity = (Double) parsedResponse.getOrDefault("popularity", -1.0);
        return popularity;
    }

}
