package galois.test.experiments.run.batch;

import galois.llm.query.INLQueryExectutor;
import galois.llm.query.ISQLExecutor;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.parser.ParserException;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import speedy.model.algebra.IAlgebraOperator;
import speedy.utility.SpeedyUtility;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRunPresidentsBatch {

    private static final String EXP_NAME = "PRESIDENTS_USA";
    //private static final String EXP_NAME = "PRESIDENTS_VENEZUELA";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "prova.txt";

    private static final ExcelExporter exportExcel = new ExcelExporter();

    private List<ExpVariant> variants = new ArrayList<>();

    public TestRunPresidentsBatch() {
        List<String> allOptimizers = List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter");
        // USA
        variants.add(new ExpVariant("Q1", "SELECT p.name, p.party from target.president p WHERE p.country='United States'", "List the name and party of USA presidents.", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q2", "SELECT p.name, p.party from target.president p WHERE p.country='United States' and  p.party='Republican'", "List the name and party of USA presidents where party is Republican", allOptimizers));
        variants.add(new ExpVariant("Q3", "SELECT count(p.party) as party from target.president p WHERE p.country='United States' and p.party='Republican'", "Count the number of US presidents where party is Republican", allOptimizers));
        variants.add(new ExpVariant("Q4", "SELECT p.name from target.president p WHERE p.country='United States' and p.party='Republican'", "List the name of USA presidents where party is Republican", allOptimizers));
        variants.add(new ExpVariant("Q5", "SELECT p.name from target.president p WHERE p.country='United States' and p.party='Republican' and p.start_year > 1980", "List the name of USA presidents after 1980 where party is Republican", allOptimizers));
        variants.add(new ExpVariant("Q6", "SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party from target.president p WHERE p.country='United States'", "List the name, the start year, the end year, the number of president and the party of USA presidents", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
//        variants.add(new ExpVariant("Q7", "SELECT p.party, count(p.party) num from target.president p WHERE p.country='United States' group by p.party order by num desc limit 1", "List the party name and the number of presidents of the party with more USA presidents", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
//        variants.add(new ExpVariant("Q8", "SELECT count(*) from target.president p where p.country='United States' and p.start_year >= 1990  and p.start_year < 2000", "count U.S. presidents who began their terms in the 1990 and finish it in 2000.", allOptimizers));
//        variants.add(new ExpVariant("Q9", "SELECT p.name from target.president p where p.country='United States' and p.party = 'Whig' order by p.end_year desc limit 1", "List the name of the last USA president where party is Whig", allOptimizers));        

        // VENEZUELA
//        variants.add(new ExpVariant("Q1", "SELECT p.name, p.party from target.president p WHERE p.country='Venezuela'", "List the name and party of Venezuela presidents.", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
//        variants.add(new ExpVariant("Q2", "SELECT p.name, p.party from target.president p WHERE p.country='Venezuela' and p.party='Liberal'", "List the name and party of Venezuela presidents where party is Liberal", allOptimizers));
//        variants.add(new ExpVariant("Q3", "SELECT count(p.party) as party from target.president p WHERE p.country='Venezuela' and p.party='Liberal'", "Count the number of Venezuela presidents where party is Liberal", allOptimizers));
//        variants.add(new ExpVariant("Q4", "SELECT p.name from target.president p WHERE p.country='Venezuela' and p.party='Liberal'", "List the name of Venezuela presidents where party is Liberal", allOptimizers));
//        variants.add(new ExpVariant("Q5", "SELECT p.name from target.president p WHERE p.country='Venezuela' and p.party='Liberal' and p.start_year > 1858", "List the name of Venezuela presidents after 1858 where party is Liberal", allOptimizers));
//        variants.add(new ExpVariant("Q6", "SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party from target.president p WHERE p.country='Venezuela'", "List the name, the start year, the end year, the number of president and the party of Venezuela presidents", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
//        variants.add(new ExpVariant("Q7", "SELECT p.party, count(p.party) num from target.president p WHERE p.country='Venezuela' group by p.party order by num desc limit 1", "List the party name and the number of presidents of the party with more Venezuela presidents", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
//        variants.add(new ExpVariant("Q8", "SELECT count(*) from target.president p where p.country='Venezuela' and p.start_year >= 1990  and p.start_year < 2000", "count Venezuela presidents who began their terms in the 1990 and finish it in 2000.", allOptimizers));
//        variants.add(new ExpVariant("Q9", "SELECT p.name from target.president p where p.country='Venezuela' and p.party = 'Military' order by p.end_year desc limit 1", "List the name of the last Venezuela president where party is Military", allOptimizers));   
    }

    @Test
    public void executeAll() {
        testParseAll();
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        String fileName = exportExcel.getFileName(EXP_NAME);
        for (ExpVariant variant : variants) {
            execute("/presidents/presidents-togetherai-nl-experiment.json", "NL", variant, metrics, results);
            execute("/presidents/presidents-togetherai-sql-experiment.json", "SQL", variant, metrics, results);
            execute("/presidents/presidents-togetherai-table-experiment.json", "TABLE", variant, metrics, results);
            execute("/presidents/presidents-togetherai-key-experiment.json", "KEY", variant, metrics, results);
            execute("/presidents/presidents-togetherai-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results);
            exportExcel.export(fileName, EXP_NAME, metrics, results);
        }
        System.out.println(SpeedyUtility.printMap(results));
    }

    @Test
    public void testParseAll() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        for (ExpVariant variant : variants) {
            parse("/presidents/presidents-togetherai-table-experiment.json", "TABLE", variant, metrics, results);
            parse("/presidents/presidents-togetherai-key-experiment.json", "KEY", variant, metrics, results);
            parse("/presidents/presidents-togetherai-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results);
        }
    }

    private void execute(String path, String type, ExpVariant variant, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Map<String, ExperimentResults> queryResults = results.get(variant.getQueryNum());
            if (queryResults == null) {
                queryResults = new HashMap<>();
                results.put(variant.getQueryNum(), queryResults);
            }
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            if (experiment.getOperatorsConfiguration().getScan().getQueryExecutor() instanceof INLQueryExectutor queryExectutor) {
                queryExectutor.setNaturalLanguagePrompt(variant.getPrompt());
            }
            if (!(experiment.getOperatorsConfiguration().getScan().getQueryExecutor() instanceof INLQueryExectutor)
                    && !(experiment.getOperatorsConfiguration().getScan().getQueryExecutor() instanceof ISQLExecutor)) {
                List<IOptimizer> optimizers = loadOptimizers(variant);
//            experiment.setOptimizers(variant.getOptimizers().stream().map(OptimizersFactory::getOptimizerByName).toList());
                experiment.setOptimizers(optimizers);
            }
            metrics.clear();
            metrics.addAll(experiment.getMetrics());
            var expResults = experiment.execute();
            log.info("Results: {}", expResults);
            for (String expKey : expResults.keySet()) {
                queryResults.put(type + "-" + expKey, expResults.get(expKey));
            }
            // Extract numbers after "Only results, same order: \n"
            String regex = "Only results, same order: \\n([\\d, ]+)-*$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(results.toString());

            if (matcher.find()) {
                String extractedResults = matcher.group(1).trim();
                saveToFile(extractedResults, RESULT_FILE_DIR + RESULT_FILE);
            } else {
                log.error("Could not extract results from log entry.");
            }
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
            //throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
    }

    private void parse(String path, String type, ExpVariant variant, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results) {
        try {
            log.info("Variant: " + variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.getQuery().setSql(variant.getQuerySql());
            IAlgebraOperator operator = experiment.parse();
            log.info("OK");    
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
    }

    private List<IOptimizer> loadOptimizers(ExpVariant variant) throws ParserException {
        List<IOptimizer> optimizers = new ArrayList<>();
        for (String optimizer : variant.getOptimizers()) {
            if (optimizer.equals("SingleConditionsOptimizerFactory")) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(variant.getQuerySql());
                for (int i = 0; i < parserWhere.getExpressions().size(); i++) {
                    optimizers.add(new IndexedConditionPushdownOptimizer(i, true));
                }
                continue;
            }
            if (optimizer.equals("SingleConditionsOptimizerFactory-WithFilter")) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(variant.getQuerySql());
                for (int i = 0; i < parserWhere.getExpressions().size(); i++) {
                    optimizers.add(new IndexedConditionPushdownOptimizer(i, false));
                }
                continue;
            }
            optimizers.add(OptimizersFactory.getOptimizerByName(optimizer));
        }
        return optimizers;
    }

    private static void saveToFile(String data, String fileName) {
        try {
            // Ensure the directory exists
            File directory = new File(RESULT_FILE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                writer.write(data);
                writer.newLine(); // Add a new line after each result for better readability
            }
        } catch (IOException e) {
            log.error("Error writing to file: " + fileName, e);
        }
    }
}
