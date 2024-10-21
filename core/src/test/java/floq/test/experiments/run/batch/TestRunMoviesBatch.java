package floq.test.experiments.run.batch;

import floq.optimizer.IOptimizer;
import floq.optimizer.IndexedConditionPushdownOptimizer;
import floq.optimizer.QueryPlan;
import floq.test.experiments.ExperimentResults;
import floq.test.experiments.json.parser.OptimizersFactory;
import floq.test.experiments.metrics.IMetric;
import floq.test.model.ExpVariant;
import floq.test.utils.ExcelExporter;
import floq.test.utils.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TestRunMoviesBatch {

    private static final String EXP_NAME = "MOVIES";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "movies-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";

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
    public void testPlanSelection() {
        double threshold = 0.9;
        boolean executeAllPlans = true;
        boolean execute = false;
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            if (execute) testRunner.execute("/movies/movies-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/movies/movies-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/movies/movies-" + executorModel + "-table-experiment.json";
            String configPathKey = "/movies/movies-" + executorModel + "-key-scan-experiment.json";
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
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-FLOQ", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-FLOQ", variant, metrics, results, optimizer);
                IOptimizer allCondition = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allCondition);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allCondition);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-FLOQ", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute) testRunner.executeSingle(configPathTable, "TABLE-FLOQ", variant, metrics, results, optimizer);
                }
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

}
