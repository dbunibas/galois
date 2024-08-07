package galois.test.utils;

import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
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
import galois.utils.GaloisDebug;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class TestRunner {

    // TODO: Turn into service
    public void execute(
            String path,
            String type,
            ExpVariant variant,
            List<IMetric> metrics,
            Map<String, Map<String, ExperimentResults>> results,
            String resultFileDir,
            String resultFile
    ) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Map<String, ExperimentResults> queryResults = results.computeIfAbsent(variant.getQueryNum(), k -> new HashMap<>());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());

            IQueryExecutor queryExecutor = experiment.getOperatorsConfiguration().getScan().getQueryExecutor();
            if (queryExecutor instanceof INLQueryExectutor nlExecutor) {
                nlExecutor.setNaturalLanguagePrompt(variant.getPrompt());
                experiment.setOptimizers(null);
            } else if (queryExecutor instanceof ISQLExecutor sqlExecutor) {
                sqlExecutor.setSql(variant.getQuerySql());
                experiment.setOptimizers(null);
            } else {
                List<IOptimizer> optimizers = loadOptimizers(variant);
                experiment.setOptimizers(optimizers);
            }
            if (experiment.getOptimizers() != null && !experiment.getOptimizers().isEmpty()) {
                log.debug("Optimizers:");
                for (IOptimizer optimizer : experiment.getOptimizers()) {
                    log.debug(optimizer.getName());
                }
            }
            GaloisDebug.log("*** Executing experiment " + experiment.toString() + " with variant: " + variant.toString() + " ***");
            metrics.clear();
            metrics.addAll(experiment.getMetrics());
            var expResults = experiment.execute();
            log.info("Results: {}", expResults);
            for (String expKey : expResults.keySet()) {
                queryResults.put(type + "-" + expKey, expResults.get(expKey));
            }

            // Extract numbers after "Only results, same order: \n"
            /*String regex = "Only results, same order: \\n([\\d, ]+)-*$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(results.toString());

            if (matcher.find()) {
                String extractedResults = matcher.group(1).trim();
                saveToFile(extractedResults, resultFileDir, resultFileDir + resultFile);
            } else {
                log.error("Could not extract results from log entry.");
            } */
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
//            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
    }

    public void executeSingle(String path, String type, ExpVariant variant, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results, IOptimizer optimizer) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Map<String, ExperimentResults> queryResults = results.computeIfAbsent(variant.getQueryNum(), k -> new HashMap<>());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());

            IQueryExecutor queryExecutor = experiment.getOperatorsConfiguration().getScan().getQueryExecutor();
            if (queryExecutor instanceof INLQueryExectutor nlExecutor) {
                nlExecutor.setNaturalLanguagePrompt(variant.getPrompt());
                experiment.setOptimizers(null);
            } else if (queryExecutor instanceof ISQLExecutor sqlExecutor) {
                sqlExecutor.setSql(variant.getQuerySql());
                experiment.setOptimizers(null);
            } else {
                if (optimizer != null) experiment.setOptimizers(List.of(optimizer));
            }
            GaloisDebug.log("*** Executing experiment " + experiment.toString() + " with variant: " + variant.toString() + " ***");
            metrics.clear();
            metrics.addAll(experiment.getMetrics());
            var expResults = experiment.executeSingle(optimizer);
            log.info("Results: {}", expResults);
            for (String expKey : expResults.keySet()) {
                queryResults.put(type + "-" + expKey, expResults.get(expKey));
            }
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
//            throw new RuntimeException("Cannot run experiment: " + path, ioe);
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

    private static void saveToFile(String data, String resultFileDir, String fileName) {
        try {
            // Ensure the directory exists
            File directory = new File(resultFileDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                writer.write(data);
                writer.newLine(); // Add a new line after each result for better readability
            }
        } catch (IOException e) {
            log.error("Error writing to file: {}", fileName, e);
        }
    }
}
