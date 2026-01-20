package galois.test.evaluation;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;
import galois.llm.query.LLMQueryStatManager;
import galois.test.experiments.metrics.IMetric;
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
            new TupleConstraintFilteredAttributes()
    );

    private static IDatabase database;
    private static List<ExperimentVariant> variants;

    @BeforeAll
    public static void beforeAll() throws IOException {
        // Load, initialize and populate the database
        SchemaDatabase schema = loadSchemaInExperimentFolder(EXPERIMENT_FOLDER_PATH);
        database = connectToPostgres(schema.getDbName(), "public", "pguser", "pguser");
        initializeDatabaseFromExperimentFolder(EXPERIMENT_FOLDER_PATH, database, schema);

        // Define the variants
        ExperimentVariant q0 = ExperimentVariant.builder()
                .queryId("Q0")
                .querySQL("SELECT r.reviewId FROM reviews r WHERE r.scoresentiment = 'POSITIVE' LIMIT 5")
                .queryUDF("SELECT r.reviewId FROM reviews r WHERE udfilter('Is the sentiment of the review {1} overall positive?', r.reviewText) LIMIT 5")
                .build();

        ExperimentVariant q1 = ExperimentVariant.builder()
                .queryId("Q1")
                .querySQL("SELECT r.reviewId, r.originalScore as score FROM reviews r")
                .queryUDF("SELECT r.reviewId, udrank('From this review {1} select a score on how much did the reviewer like the movie based on provided rubrics. Rubrics: 5 (Very positive): Strong positive sentiment, indicating high satisfaction. 4 (Positive): Noticeably positive sentiment, indicating general satisfaction. 3 (Neutral): Expresses no clear positive or negative sentiment. May be factual or descriptive without emotional language. 2 (Negative): Noticeably negative sentiment, indicating some level of dissatisfaction but without strong anger or frustration. 1 (Very negative): Strong negative sentiment, indicating high dissatisfaction, frustration, or anger', r.reviewText) as score FROM reviews r ")
                .build();

        variants = List.of(q0);
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

