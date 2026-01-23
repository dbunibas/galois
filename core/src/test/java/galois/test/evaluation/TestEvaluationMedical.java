package galois.test.evaluation;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;

import galois.llm.query.LLMQueryStatManager;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class TestEvaluationMedical {

    private static final IUserDefinedFunctionFactory GALOIS_UDF_FACTORY = new GaloisUDFFactory();

    // Experiment name
    private static final String EXPERIMENT_NAME = "Medical";
    // Experiment folder path starting from resources
    private static final String EXPERIMENT_FOLDER_PATH = "/evaluation/sem-bench-medical";

    //private static final String RESULT_FILE_DIR = "src/test/evaluation/results/";
    //private static final String RESULT_FILE = "medical-results.txt";

    //private static final TestRunner testRunner = new TestRunner();
    //private static final ExcelExporter exportExcel = new ExcelExporter();

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
        initializeDatabaseFromExperimentFolder(EXPERIMENT_FOLDER_PATH, database, schema);

        // Define the variants
        ExperimentVariant q0 = ExperimentVariant.builder()
                .queryId("Q0")
                .querySQL("SELECT m.patient_id FROM medical m WHERE m.text_diagnosis = 'allergy'")
                .queryUDF("SELECT m.patient_id FROM medical m WHERE udfilter('Patient with these: {1} symptoms has an allergy?', m.text_symptoms)")
                .build();
        ExperimentVariant q1 = ExperimentVariant.builder()
                .queryId("Q1")
                .querySQL("SELECT m.patient_id FROM medical m WHERE m.text_diagnosis = 'acne'")
                .queryUDF("SELECT m.patient_id FROM medical m WHERE udfilter('Patient with these: {1} symptoms has acne?', m.text_symptoms)")
                .build();
        // ExperimentVariant q1 = ExperimentVariant.builder()
        //         .queryId("Q1")
        //         .querySQL("SELECT m.patient_id FROM medical m WHERE m.is_sick = 'True'")
        //         .queryUDF("SELECT m.patient_id FROM medical m WHERE udfilter('Patient with these: {1} symptoms is sick?', m.text_symptoms)")
        //         .build();
        // Equivalent of query 10 from sembench
        ExperimentVariant q2 = ExperimentVariant.builder()
                .queryId("Q2")
                .querySQL("SELECT m.patient_id, m.text_diagnosis FROM medical m")
                .queryUDF("SELECT m.patient_id, udmap('Classify these symptoms: {1};  to one of given diseases: malaria,gastroesophageal reflux disease,impetigo,dimorphic hemorrhoids,peptic ulcer disease,bronchial asthma,fungal infection,cervical spondylosis,typhoid,common cold,hypertension,diabetes,dengue,chicken pox,migraine,pneumonia,urinary tract infection,arthritis,psoriasis,varicose veins,allergy,acne,drug reaction,jaundice. Reply in lower case', m.text_symptoms) as text_diagnosis FROM medical m ")
                .build();
        variants = List.of(q0, q1, q2);
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


