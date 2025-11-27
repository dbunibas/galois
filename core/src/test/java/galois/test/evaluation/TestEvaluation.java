package galois.test.evaluation;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import galois.test.experiments.Query;
import galois.optimizer.IOptimizer;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.TupleCardinalityMetric;
import galois.test.experiments.metrics.TupleCellSimilarityFilteredAttributes;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import galois.test.utils.TestUtils;
import galois.udf.GaloisUDFFactory;
import galois.utils.GaloisDebug;
import speedy.exceptions.DAOException;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAODBMSDatabase;
import speedy.persistence.DAOMainMemoryDatabase;
import speedy.persistence.Types;
import speedy.persistence.file.CSVFile;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import static galois.test.evaluation.DatabaseFactory.connectToPostgres;
import static galois.test.evaluation.DatabaseInitializer.initializeDatabaseFromExperimentFolder;
import static galois.test.evaluation.SchemaLoader.loadSchemaInExperimentFolder;

@Slf4j
public class TestEvaluation {
    private static final IUserDefinedFunctionFactory GALOIS_UDF_FACTORY = new GaloisUDFFactory();
    private static IDatabase mainMemoryDB;
    // Experiment name
    private static final String EXPERIMENT_NAME = "SemBenchMovies";
    // Experiment folder path starting from resources
    private static final String EXPERIMENT_FOLDER_PATH = "/evaluation/sem-bench-movies";

    private static final String RESULT_FILE_DIR = "src/test/evaluation/results/";
    private static final String RESULT_FILE = "movie-reviews-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();
    
    private String executorModel = "togetherai";
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
        Boolean forceDropDB = false;
        if (forceDropDB){
            forceDropDatabase(schema.getDbName(), "pguser", "pguser");
        }
        database = connectToPostgres(schema.getDbName(), "public", "pguser", "pguser");
        initializeDatabaseFromExperimentFolder(EXPERIMENT_FOLDER_PATH, database, schema);
        mainMemoryDB = new DAOMainMemoryDatabase().loadCSVDatabase("src/test/resources"+EXPERIMENT_FOLDER_PATH+"/data", ',', '"', false, true);
    
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


        // 0. Definire schema DB e dati (in resources cartella con schema -> file json, dati -> file csv)
        //  - Schema: nome DB, list tabelle - ogni tabella lista di attributi
        //  - Connessione: parserizzare la AccessConfiguration
        // Ref. generazione e caricamento dati Experiment::createDatabaseForExpected


        // 1. Definire le query (nuova versione ExpVariant)
        //   - Obbligatorie: queryNum, querySQL - per GT, queryUDF - per valutazione
        //   - Opzionali: optimizers - default vuoti, prompt - NL per retrocompatibilità
        // test parsing queries
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        for (ExperimentVariant variant : variants) {
            log.info("Parsing query {}", variant.getQueryId());
            IAlgebraOperator result = assertDoesNotThrow(() -> {
                IAlgebraOperator res = sqlQueryParser.parse(variant.getQuerySQL());
                log.info("Parsed result:\n{}", res);
                return res; // Restituisce l'oggetto al di fuori della lambda
            });
            AccessConfiguration accessConfig = ((DBMSDB) database).getAccessConfiguration();
            ResultSet resultSet = QueryManager.executeQuery(variant.getQuerySQL(), accessConfig);
            ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
            List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
            expectedITerator.close();
            log.info("Expected size: {}", expectedResults.size());
            log.info("Query ID: {} - Ground Truth Size: {}", variant.getQueryId(), expectedResults.size());
                
            // Se vuoi stampare le tuple per debug:
            if (!expectedResults.isEmpty()) {
                log.debug("Sample tuple: {}", expectedResults.get(0));
            }
            IAlgebraOperator operator = new SQLQueryParser().parse(variant.getQueryUDF(), GALOIS_UDF_FACTORY);
            ITupleIterator resultudf = operator.execute(mainMemoryDB, mainMemoryDB);
            List<Tuple> actualResults=TestUtils.toTupleList(resultudf);
            resultudf.close();
            log.info("Query ID: {} - UDF Result Size: {}", variant.getQueryId(), actualResults.size());
        }


    };
    
        // 2. Valutazione
        // 2.1 Creare e popolare automaticamente il DB (vedi 0. Ref.)
        // 2.2 Eseguire querySQL per ottenere la ground truth 
        // 2.3 Eseguire queryUDF per expected
        // 2.4 Confrontare i risultati con le metriche DEFAULT_METRICS
        // Ref. Experiment::executeSingle
    

        // 3. Export dei risultati
        private static void forceDropDatabase(String dbName, String user, String password) {
            String adminUrl = "jdbc:postgresql://localhost:5432/postgres"; // Ci connettiamo al DB di sistema
            
            log.info("--- FORCING DROP OF DATABASE {} ---", dbName);
            
            try (Connection conn = DriverManager.getConnection(adminUrl, user, password);
                 Statement stmt = conn.createStatement()) {
    
                // 1. Termina le connessioni esistenti al DB (altrimenti il DROP fallisce)
                String killConnections = "SELECT pg_terminate_backend(pg_stat_activity.pid) " +
                                         "FROM pg_stat_activity " +
                                         "WHERE pg_stat_activity.datname = '" + dbName + "' " +
                                         "AND pid <> pg_backend_pid();";
                try {
                    stmt.execute(killConnections);
                } catch (SQLException e) {
                    log.warn("Warning during connection kill (safe to ignore if db doesn't exist): {}", e.getMessage());
                }
    
                // 2. Droppa il database
                stmt.executeUpdate("DROP DATABASE IF EXISTS \"" + dbName + "\"");
                log.info("Database {} dropped successfully.", dbName);
    
            } catch (SQLException e) {
                log.error("Errore durante il drop manuale del DB: {}", e.getMessage());
                // Non lanciamo eccezione bloccante qui, lasciamo che sia l'initializer a fallire se c'è un problema grave,
                // oppure potrebbe essere che il DB non esisteva proprio.
            }
        }
}

