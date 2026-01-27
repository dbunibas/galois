package galois.test.evaluation;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;
import galois.llm.query.LLMQueryStatManager;
import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.SpearmanCorrelation;
import galois.test.experiments.metrics.TupleCardinalityMetric;
import galois.test.experiments.metrics.TupleConstraintFilteredAttributes;
import galois.test.experiments.metrics.TupleLLMSimilarityConstraintFilteredAttributes;
import galois.udf.GaloisUDFFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.io.IOException;
import java.util.List;
import galois.test.experiments.metrics.CellF1Score;

import static galois.test.evaluation.DatabaseFactory.connectToPostgres;
import static galois.test.evaluation.DatabaseInitializer.initializeDatabaseFromExperimentFolder;
import static galois.test.evaluation.SchemaLoader.loadSchemaInExperimentFolder;
import static galois.test.utils.TestUtils.toTupleList;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class TestEvaluationMovies {
    private static final IUserDefinedFunctionFactory GALOIS_UDF_FACTORY = new GaloisUDFFactory();

    // Experiment name
    private static final String EXPERIMENT_NAME = "SemBenchMovies";
    // Experiment folder path starting from resources
    private static final String EXPERIMENT_FOLDER_PATH = "/evaluation/sem-bench-movies";

    // Default metrics to evaluate
    private static final List<IMetric> DEFAULT_METRICS = List.of(
        new TupleCardinalityMetric(),
        new SpearmanCorrelation(),
        new CellF1Score()

    );

    private static IDatabase database;
    private static List<ExperimentVariant> variants;

    @BeforeAll
    public static void beforeAll() throws IOException {
        // Load, initialize and populate the database
        SchemaDatabase schema = loadSchemaInExperimentFolder(EXPERIMENT_FOLDER_PATH);
        database = connectToPostgres(schema.getDbName(), "public", "pguser", "pguser");
        initializeDatabaseFromExperimentFolder(EXPERIMENT_FOLDER_PATH, database, schema, true);

        // Define the variants
        ExperimentVariant q1 = ExperimentVariant.builder()
                .queryId("Q1")
                .querySQL("SELECT r.reviewid FROM reviews r WHERE r.scoresentiment = 'POSITIVE' LIMIT 5")
                .queryUDF("SELECT r.reviewid FROM reviews r WHERE udfilter('Is the sentiment of the review {1} clearly positive?', r.reviewText) LIMIT 5")
                .build();
        ExperimentVariant q2 = ExperimentVariant.builder()
                .queryId("Q2")
                .querySQL("SELECT r.reviewid FROM reviews r WHERE r.filmTitle='taken_3' AND r.scoresentiment = 'POSITIVE' LIMIT 5")
                .queryUDF("SELECT r.reviewid FROM reviews r WHERE r.filmTitle='taken_3' AND udfilter('Is the sentiment of the review {1} clearly positive?', r.reviewText) LIMIT 5")
                .build();
        ExperimentVariant q3 = ExperimentVariant.builder()
                .queryId("Q3")
                .querySQL("SELECT COUNT(*) as positive_review_cnt FROM reviews r WHERE r.filmTitle='taken_3' AND r.scoresentiment = 'POSITIVE'")
                .queryUDF("SELECT COUNT(*) as positive_review_cnt FROM reviews r WHERE r.filmTitle='taken_3' AND udfilter('Is the sentiment of the review {1} clearly positive?', r.reviewText)")
                .build();
        // ExperimentVariant q4 = ExperimentVariant.builder()
        //         .queryId("Q4")
        //         .querySQL("SELECT CAST(SUM(CASE WHEN scoreSentiment = 'POSITIVE' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) AS positivity_ratio FROM reviews r  WHERE filmTitle = 'taken_3';")
        //         .queryUDF("SELECT COUNT(*) as positive_review_cnt FROM reviews r WHERE r.filmTitle='taken_3' AND udfilter('Is the sentiment of the review {1} clearly positive?', r.reviewText)")
        //         .build();
        ExperimentVariant q8 = ExperimentVariant.builder()
                .queryId("Q8")
                .querySQL("SELECT r.scoresentiment, COUNT(*) as review_cnt FROM reviews r WHERE r.filmTitle='taken_3' GROUP BY r.scoresentiment")
//                .queryUDF("SELECT scoresentiment2 as scoresentiment, COUNT(*) as review_cnt FROM (SELECT udmap('Answer POSITIVE if the sentiment expressed by this review: {1} is clearly positive. Otherwise answer NEGATIVE. The answer must be in capslock and without spaces', r.reviewText) as scoresentiment2 FROM reviews r WHERE r.filmTitle='taken_3') AS t GROUP BY scoresentiment2")
                .queryUDF("SELECT udmap('Answer POSITIVE if the sentiment expressed by this review: {1} is clearly positive. Otherwise answer NEGATIVE. The answer must be in capslock and without spaces', reviewText), COUNT(*) as review_cnt FROM reviews r WHERE filmTitle='taken_3' GROUP BY udmap")
                .build();
        ExperimentVariant q9 = ExperimentVariant.builder()
                .queryId("Q9")
                .querySQL("SELECT r.reviewId, r.originalScore as score FROM reviews r WHERE r.filmTitle='ant_man_and_the_wasp_quantumania'")
                .queryUDF("SELECT r.reviewId, udrank('From this review {1} select a score on how much did the reviewer like the movie based on provided rubrics. Rubrics: 5 (Very positive): Strong positive sentiment, indicating high satisfaction. 4 (Positive): Noticeably positive sentiment, indicating general satisfaction. 3 (Neutral): Expresses no clear positive or negative sentiment. May be factual or descriptive without emotional language. 2 (Negative): Noticeably negative sentiment, indicating some level of dissatisfaction but without strong anger or frustration. 1 (Very negative): Strong negative sentiment, indicating high dissatisfaction, frustration, or anger', r.reviewText) as score FROM reviews r WHERE r.filmTitle='ant_man_and_the_wasp_quantumania'")
                .build();
        ExperimentVariant q10 = ExperimentVariant.builder()
                .queryId("Q10")
                .querySQL("SELECT r.filmTitle, AVG(r.originalScore) as movieScore FROM reviews r GROUP BY r.filmTitle")
                .queryUDF("SELECT r.filmTitle, AVG(udrank('From this review {1} select a score on how much did the reviewer like the movie based on provided rubrics. Rubrics: 5 (Very positive): Strong positive sentiment, indicating high satisfaction. 4 (Positive): Noticeably positive sentiment, indicating general satisfaction. 3 (Neutral): Expresses no clear positive or negative sentiment. May be factual or descriptive without emotional language. 2 (Negative): Noticeably negative sentiment, indicating some level of dissatisfaction but without strong anger or frustration. 1 (Very negative): Strong negative sentiment, indicating high dissatisfaction, frustration, or anger', r.reviewText)) as movieScore FROM reviews r GROUP BY r.filmTitle")
                .build();   

        variants = List.of(q4);
    }

    @Test
    public void testCanParseQueries() {
        SQLQueryParser sqlQueryParser = new SQLQueryParser();

        for (ExperimentVariant variant : variants) {
            assertNotNull(sqlQueryParser.parse(variant.getQuerySQL()));
            assertNotNull(sqlQueryParser.parse(variant.getQueryUDF(), GALOIS_UDF_FACTORY));
        }
    }

    @Test
    public void testEvaluation() {
        EvaluationResults evaluationResults = new EvaluationResults();
        SQLQueryParser sqlQueryParser = new SQLQueryParser();

        for (ExperimentVariant variant : variants) {
            LLMQueryStatManager.getInstance().resetStats();

            long startTime = System.currentTimeMillis();
            IAlgebraOperator gtOperator = sqlQueryParser.parse(variant.getQuerySQL());
            List<Tuple> expected = toTupleList(gtOperator.execute(database, database));
            log.debug("Expected: {}", expected);
            IAlgebraOperator operator = new SQLQueryParser().parse(variant.getQueryUDF(), GALOIS_UDF_FACTORY);
            List<Tuple> results = toTupleList(operator.execute(database, database));

            EvaluationResult evaluationResult = new EvaluationResult(
                    EXPERIMENT_NAME,
                    variant,
                    startTime,
                    expected,
                    results,
                    DEFAULT_METRICS
            );
            evaluationResult.computeScores(database);
            evaluationResults.appendResult(evaluationResult);
            log.info("**** {}", evaluationResult);
        }

        evaluationResults.exportAsText(EXPERIMENT_NAME);
        evaluationResults.exportAsExcel(EXPERIMENT_NAME);
    }
}

