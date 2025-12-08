package galois.test.evaluation;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;
import galois.test.experiments.metrics.*;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.test.utils.TestUtils;
import galois.udf.GaloisUDFFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.io.IOException;
import java.util.List;

import static galois.test.evaluation.DatabaseFactory.connectToMainMemoryCSV;
import static galois.test.evaluation.DatabaseFactory.connectToPostgres;
import static galois.test.evaluation.DatabaseInitializer.initializeDatabaseFromExperimentFolder;
import static galois.test.evaluation.SchemaLoader.loadSchemaInExperimentFolder;
import static galois.test.utils.TestUtils.toTupleList;

@Slf4j
public class TestEvaluationArtists {
    private static final IUserDefinedFunctionFactory GALOIS_UDF_FACTORY = new GaloisUDFFactory();

    // Experiment name
    private static final String EXPERIMENT_NAME = "SemBenchMovies";
    // Experiment folder path starting from resources
    private static final String EXPERIMENT_FOLDER_PATH = "/evaluation/kaggle-artist";

    private static final String RESULT_FILE_DIR = "src/test/evaluation/results/";
    private static final String RESULT_FILE = "movie-reviews-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private String executorModel = "togetherai";

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
    //    database = connectToMainMemoryCSV(TestEvaluation.class.getResource(EXPERIMENT_FOLDER_PATH).getPath() + "/data", ',', '"', true);
        initializeDatabaseFromExperimentFolder(EXPERIMENT_FOLDER_PATH, database, schema, true);

        // Define the variants
        ExperimentVariant q0 = ExperimentVariant.builder()
                .queryId("Q0")
                .querySQL("SELECT a.artistId, a.name FROM artists a WHERE a.birthYear = 1941")
                .queryUDF("SELECT a.artistId, a.name FROM artists a WHERE udfilter('Is the artist called {1} born in 1941?', a.name)")
                .build();
        ExperimentVariant q1 = ExperimentVariant.builder()
                .queryId("Q1")
                .querySQL("SELECT a.artistId, a.name FROM artists a WHERE a.nationality = 'American'")
                .queryUDF("SELECT a.artistId, a.name FROM artists a WHERE udfilter('Is the artist called {1} American?', a.name)")
                .build();

        variants = List.of(q0,q1);
    }

    @Test
    public void testCanParseQueries() {

    }

    @Test
    public void testEvaluation() {
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        for (ExperimentVariant variant : variants) {
            log.info("Parsing query {}", variant.getQueryId());
            IAlgebraOperator gtOperator = sqlQueryParser.parse(variant.getQuerySQL());
            List<Tuple> expected = toTupleList(gtOperator.execute(database, database));
            log.info("**** Expected: {}", expected);

            IAlgebraOperator operator = new SQLQueryParser().parse(variant.getQueryUDF(), GALOIS_UDF_FACTORY);
            List<Tuple> results = TestUtils.toTupleList(operator.execute(database, database));
            log.info("**** Result: {}", results);
            
            for (IMetric metric : DEFAULT_METRICS) {
                Double score = metric.getScore(database, expected, results);
                log.info("**** {}: {} has score {}", variant.getQueryId(), metric.getName(), score);
            }
        }
    }
}


