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
public class TestRunRAGFortuneBatch {

    private static final String EXP_NAME = "RAG-Fortune";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "rag-fortune-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;
    private String executorModel = "llama3";

    public TestRunRAGFortuneBatch() {
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
                .querySql("select f.rank, f.company from fortune_2024 f order by f.rank asc limit 10")
                .prompt("List the rank and company names of the top 10 companies according to the Fortune 2024 ranking.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("select company, ceo from fortune_2024 f where f.headquartersstate = 'Oklahoma'")
                .prompt("Give me a list of all companies and their CEOs that are headquartered in Oklahoma, according to the 'fortune_2024' data.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("select company, headquartersstate from fortune_2024 where number_of_employees > 1000000")
                .prompt("List the company names and the states where they are headquartered for any companies in the Fortune 2024 list that have more than one million employees")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select headquarterscity from fortune_2024 f where f.industry = 'Airlines'")
                .prompt("List the names of the cities where the headquarters of all the airline companies in the Fortune 2024 list are located")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select company from fortune_2024 f where f.sector = 'Technology' and founder_is_ceo = true and is_profitable = true")
                .prompt("List the companies from the Fortune 2024 list that are in the Technology sector, are profitable, and have their founder as the current CEO")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("select ceo from fortune_2024 f where  is_femaleceo = true and f.companytype = 'Private'")
                .prompt("List the CEOs from the Fortune 2024 list who are female and lead privately held companies")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select company from fortune_2024 where best_companies_to_work_for = true and industry = 'Airlines'")
                .prompt("List the airline companies from the Fortune 2024 list that are also included in the 'Best Companies to Work For' ranking")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select company, ceo from fortune_2024 f where f.is_profitable = true and f.is_femaleceo = true and f.headquartersstate = 'Texas'")
                .prompt("List the companies in Texas with female CEOs that were profitable in 2024, according to the Fortune 2024 list.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where company = 'Nvidia'")
                .prompt("List the company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees for the company named 'Nvidia' from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q10 = ExpVariant.builder()
                .queryNum("Q10")
                .querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where headquarterscity = 'Santa Clara' and rank < 70")
                .prompt("List company name, headquarters state, stock ticker, CEO, whether the founder is the CEO, whether the CEO is female, and the number of employees, for companies headquartered in 'Santa Clara' with a rank below 70 from the 'fortune_2024' database.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        variants = List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10);
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
            if (execute) testRunner.execute("/rag-fortune/fortune2024-" + executorModel + "-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            if (execute) testRunner.execute("/rag-fortune/fortune2024-" + executorModel + "-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            String configPathTable = "/rag-fortune/fortune2024-" + executorModel + "-table-experiment.json";
            String configPathKey = "/rag-fortune/fortune2024-" + executorModel + "-key-scan-experiment.json";
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
