package galois.test.evaluation;

import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.TupleCardinalityMetric;
import galois.test.experiments.metrics.TupleCellSimilarityFilteredAttributes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.database.IDatabase;

import java.io.IOException;
import java.util.List;

import static galois.test.evaluation.DatabaseFactory.connectToPostgres;
import static galois.test.evaluation.DatabaseInitializer.initializeDatabaseFromExperimentFolder;
import static galois.test.evaluation.SchemaLoader.loadSchemaInExperimentFolder;

@Slf4j
public class TestEvaluation {
    // Experiment name
    private static final String EXPERIMENT_NAME = "SemBenchMovies";
    // Experiment folder path starting from resources
    private static final String EXPERIMENT_FOLDER_PATH = "/evaluation/sem-bench-movies";

    // Default metrics to evaluate
    private static final List<IMetric> DEFAULT_METRICS = List.of(
            new TupleCardinalityMetric(),
            new TupleCellSimilarityFilteredAttributes()
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
                .querySQL("SELECT r.id FROM reviews r WHERE r.scoresentiment = 'POSITIVE'")
                .queryUDF("SELECT r.id FROM reviews r WHERE udfilter('Is the sentiment of the review {1} positive?', r.reviewtext)")
                .build();
        variants = List.of(q0);
    }

    @Test
    public void testEvaluation() {
        // 2. Valutazione
        // 2.1 Eseguire querySQL per ottenere la ground truth
        // 2.2 Eseguire queryUDF per expected
        // 2.3 Confrontare i risultati con le metriche DEFAULT_METRICS

        // 3. Export dei risultati
    }
}
