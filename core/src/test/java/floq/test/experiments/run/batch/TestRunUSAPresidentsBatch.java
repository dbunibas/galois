package floq.test.experiments.run.batch;

import com.floq.sqlparser.SQLQueryParser;
import floq.test.experiments.json.parser.ExperimentParser;
import floq.test.experiments.json.parser.OptimizersFactory;
import floq.Constants;
import floq.llm.models.TogetherAIModel;
import floq.llm.models.togetherai.TogetherAIConstants;
import floq.llm.query.utils.QueryUtils;
import floq.optimizer.IOptimizer;
import floq.optimizer.IndexedConditionPushdownOptimizer;
import floq.optimizer.QueryPlan;
import floq.parser.ParserWhere;
import floq.test.experiments.Experiment;
import floq.test.experiments.ExperimentResults;
import floq.test.experiments.metrics.IMetric;
import floq.test.model.ExpVariant;
import floq.test.utils.ExcelExporter;
import floq.test.utils.TestRunner;
import floq.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import engine.model.algebra.IAlgebraOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Key;
import static engine.utility.EngineUtility.printMap;

@Slf4j
public class TestRunUSAPresidentsBatch {

    private static final String EXP_NAME = "USA_PRESIDENTS";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "usa-presidents-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";
//    private String executorModel = "" + executorModel + "";

    public TestRunUSAPresidentsBatch() {
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
                .querySql("SELECT p.name, p.party FROM target.world_presidents p WHERE p.country='United States'")
                .prompt("List the name and party of USA presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name and party of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("Count the number of US presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Republican' AND p.start_year > 1980")
                .prompt("List the name of USA presidents after 1980 where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party FROM target.world_presidents p WHERE p.country='United States'")
                .prompt("List the name, the start year, the end year, the number of president and the party of USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num FROM target.world_presidents p WHERE p.country='United States' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) FROM target.world_presidents p WHERE p.country='United States' AND p.start_year >= 1990  AND p.start_year < 2000")
                .prompt("count U.S. presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name FROM target.world_presidents p WHERE p.country='United States' AND p.party='Whig' order by p.end_year desc limit 1")
                .prompt("List the name of the last USA president where party is Whig")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1850 AND end_year < 1900 AND party ='Democratic'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Democratic USA president who served between 1850 and 1900")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q11 = ExpVariant.builder()
                .queryNum("Q11")
                .querySql("SELECT p.name, p.party, start_year, end_year, p.cardinal_number FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 AND party ='Democratic'")
                .prompt("List the name, the party, the start and end year and the cardinal number of Democratic USA president who served between 1850 and 1900")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q12 = ExpVariant.builder()
                .queryNum("Q12")
                .querySql("SELECT party, count(*) num FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1800 AND end_year < 1900 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a USA president between the 1800 and 1900.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        
        ExpVariant q13 = ExpVariant.builder()
                .queryNum("Q13")
                .querySql("SELECT party, count(*) num FROM target.world_presidents p WHERE p.country ='United States' AND start_year > 1900 AND end_year < 2000 group by p.party order by num desc")
                .prompt("List the party name and the number of times that the party have elected a USA president between the 1900 and 2000.")
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
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                IOptimizer allCondition = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"); //remove algebra true
                if (execute) testRunner.executeSingle(configPathTable, "TABLE-ALL-CONDITIONS", variant, metrics, results, allCondition);
                if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-ALL-CONDITIONS", variant, metrics, results, allCondition);
            } else {
                if (confidenceKeys != null && confidenceKeys > threshold) {
                    // Execute KEY-SCAN
                    if (execute) testRunner.executeSingle(configPathKey, "KEY-SCAN-GALOIS", variant, metrics, results, optimizer);
                } else {
                    // Execute TABLE
                    if (execute) testRunner.executeSingle(configPathTable, "TABLE-GALOIS", variant, metrics, results, optimizer);
                }
            }
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
    }

}
