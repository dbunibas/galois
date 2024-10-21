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
public class TestRunVenezuelaPresidentsBatch {

    private static final String EXP_NAME = "VENEZUELA_PRESIDENTS";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "venezuela-presidents-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";

    public TestRunVenezuelaPresidentsBatch() {
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
                .querySql("SELECT DISTINCT p.name, p.party FROM target.world_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name and party of Venezuela presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party FROM target.world_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("List the name and party of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) as party FROM target.world_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("Count the number of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("List the name of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='Venezuela' AND p.party='Liberal' AND p.start_year > 1858")
                .prompt("List the name of Venezuela presidents after 1858 where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party FROM target.world_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name, the start year, the end year, the number of president and the party of Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num FROM target.world_presidents p WHERE p.country='Venezuela' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) FROM target.world_presidents p where p.country='Venezuela' AND p.start_year >= 1990  AND p.start_year < 2000")
                .prompt("count Venezuela presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name FROM target.world_presidents p where p.country='Venezuela' AND p.party = 'Military' order by p.end_year desc limit 1")
                .prompt("List the name of the last Venezuela president where party is Military")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='Venezuela' AND start_year > 1900 AND end_year < 2000 AND party ='Democratic Action'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Democratic Action Venezuela president who served between 1900 and 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='Venezuela' AND start_year > 1800 AND end_year < 1900 AND party ='Conservative'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Conservative president who served between 1800 and 1900")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q12 = ExpVariant.builder()
                .queryNum("Q12")
                .querySql("SELECT p.party, count(p.party) num FROM target.world_presidents p WHERE p.country ='Venezuela' AND start_year > 1800 AND end_year < 1900 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a Venezuela president between the 1800 and 1900.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q13 = ExpVariant.builder()
                .queryNum("Q13")
                .querySql("SELECT party, count(p.party) num FROM target.world_presidents p WHERE p.country ='Venezuela' AND start_year > 1900 AND end_year < 2000 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a Venezuela president between the 1900 and 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13);
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
            if (execute) testRunner.execute("/presidents/presidents-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/presidents/presidents-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/presidents/presidents-" + executorModel + "-table-experiment.json";
            String configPathKey = "/presidents/presidents-" + executorModel + "-key-scan-experiment.json";
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
