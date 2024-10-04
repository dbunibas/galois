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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Key;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunMoviesBatch {

    private static final String EXP_NAME = "MOVIES";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "movies-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunMoviesBatch() {
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
                .querySql("SELECT m.originaltitle FROM target.movie m WHERE m.director='Richard Thorpe'")
                .prompt("List the title of the movies directed by Richard Thorpe")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT m.originaltitle FROM target.movie m WHERE m.director='Steven Spielberg'")
                .prompt("List the title of the movies directed by Steven Spielberg")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT m.originaltitle, m.startyear FROM target.movie m WHERE m.director='Richard Thorpe' AND m.startyear > 1950")
                .prompt("List the title and year of the movies directed by Richard Thorpe after the 1950")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT m.originaltitle, m.startyear FROM target.movie m WHERE m.director='Steven Spielberg' AND m.startyear > 2000")
                .prompt("List the title and year of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT m.originaltitle, m.startyear, m.genres, m.birthyear FROM target.movie m WHERE m.director='Steven Spielberg' AND m.startyear > 2000")
                .prompt("List the title, year, genres and birthyear of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT m.originaltitle, m.startyear, m.genres, m.birthyear, m.deathyear, m.runtimeminutes FROM target.movie m WHERE m.director = 'Steven Spielberg' AND m.startyear > 1990 AND m.startyear < 2000")
                .prompt("List the title, year, genres, birthyear, deathyear and runtimeminutes of the movies directed by Steven Spielberg between the 1990 and the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT m.startyear, count(*) as numMovies FROM target.movie m WHERE m.director = 'Steven Spielberg' AND m.startyear is not null group by m.startyear")
                .prompt("List the year and the number of produced movies in that year directed by Steven Spielberg.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT m.startyear, count(*) as count FROM target.movie m WHERE m.director = 'Tim Burton' group by m.startyear order by count desc limit 1")
                .prompt("Return the most prolific year of Tim Burton")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT m.director, (m.startyear - m.birthyear) as director_age FROM target.movie m WHERE m.startyear is not null AND m.birthyear is not null order by director_age desc limit 1")
                .prompt("Return the oldest film director")
                .optimizers(multipleConditionsOptimizers)
                .build();

        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
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
            testRunner.execute("/movies/movies-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/movies/movies-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/movies/movies-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/movies/movies-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
//            testRunner.execute("/movies/movies-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
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
        String configPath = "/movies/movies-llama3-table-experiment.json";
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
            String configPath = "/movies/movies-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimatorSchema(configPath, variant);
            break;
        }
    }
    
    @Test
    public void testConfidenceEstimator() {
        // confidence for every attribute
        for (ExpVariant variant : variants) {
            String configPath = "/movies/movies-llama3-table-experiment.json";
            testRunner.executeConfidenceEstimator(configPath, variant);
            break;
        }
    }
        
    @Test
    public void testConfidenceEstimatorQuery() {
        for (ExpVariant variant : variants) {
            String configPath = "/movies/movies-llama3-table-experiment.json";
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
            testRunner.execute("/movies/movies-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/movies/movies-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String tableExp = "/movies/movies-llama3-table-experiment.json";
            String keyExp = "/movies/movies-llama3-key-scan-experiment.json";
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
