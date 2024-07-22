package galois.test.experiments.run;

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
public class TestRunGeo {

    private static final String EXP_NAME = "GEO";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "prova.txt";

    private static final ExcelExporter exportExcel = new ExcelExporter();

    private List<ExpVariant> variants = new ArrayList<>();

    public TestRunGeo() {
        variants.add(new ExpVariant("Q1", "SELECT state_name, population, area FROM target.usa_state", "List the state name, population and area from USA states", List.of()));
        variants.add(new ExpVariant("Q2", "SELECT us.state_name, us.capital, us.area FROM target.usa_state us", "List the state name, capital and area from USA states", List.of()));
        variants.add(new ExpVariant("Q3", "SELECT state_name, population, area FROM target.usa_state where capital = 'Frankfort'", "List the state name, population and area from USA states where the capital is Frankfort", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q4", "SELECT us.state_name, us.population, us.capital FROM target.usa_state us where us.population > 5000000", "List the state name, population and capital from USA states where the population is greater than 5000000", List.of("AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q5", "SELECT us.state_name, us.capital FROM target.usa_state us where us.population > 5000000 and us.density < 1000", "List the state name and capital from USA states where the population is greater than 5000000 and the density is lower than 1000", List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q6", "SELECT us.state_name, us.capital, us.density, us.population FROM target.usa_state us where us.population > 5000000 and us.density < 1000 and us.area < 50000", "List the state name, capital, density and population from USA states where the population is greater than 5000000, the density is lower than 1000 and the area is lower than 50000", List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q7", "SELECT us.state_name, us.capital FROM target.usa_state us WHERE us.population > 3000000 and us.area > 50000 ORDER BY us.capital", "List the state name and capital ordered by capital from USA states where the population is greater than 3000000 and the area is greater than 50000", List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q8", "SELECT us.state_name, us.capital, us.population FROM target.usa_state us WHERE us.population > 3000000 and us.population < 8000000 and us.area > 50000 ORDER BY us.population", "List the state name capital and population ordered by population from USA states where the population is greater than 3000000 and lower than 8000000 and the area is greater than 50000", List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
        variants.add(new ExpVariant("Q9", "SELECT us.state_name, us.capital, us.population, us.area FROM target.usa_state us WHERE us.population = 47000000 and us.area = 56153", "List the state name, the capital, the popoulation and the area from USA states where the population is 4700000 and the are is 56153", List.of("SingleConditionsOptimizerFactory", "SingleConditionsOptimizerFactory-WithFilter", "AllConditionsPushdownOptimizer", "AllConditionsPushdownOptimizer-WithFilter")));
    }

    @Test
    public void executeAll() {
        testParseAll();
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        for (ExpVariant variant : variants) {
            execute("/geo_data/geo-llama3-nl-experiment.json", "NL", variant, metrics, results);
            execute("/geo_data/geo-llama3-sql-experiment.json", "SQL", variant, metrics, results);
            execute("/geo_data/geo-llama3-table-experiment.json", "TABLE", variant, metrics, results);
            execute("/geo_data/geo-llama3-key-experiment.json", "KEY", variant, metrics, results);
            execute("/geo_data/geo-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results);
        }
        System.out.println(SpeedyUtility.printMap(results));
        exportExcel.export(EXP_NAME, metrics, results);
    }

//    @Test
    public void testParseAll() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        for (ExpVariant variant : variants) {
            parse("/geo_data/geo-llama3-table-experiment.json", "TABLE", variant, metrics, results);
//            parse("/geo_data/geo-llama3-key-experiment.json", "KEY", variant, metrics, results);
//            parse("/geo_data/geo-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results);
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
            throw new RuntimeException("Cannot run experiment: " + path, ioe);
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
