package floq.test.utils;

import floq.test.experiments.ExperimentResults;
import floq.test.experiments.json.parser.ExperimentParser;
import floq.test.experiments.json.parser.OptimizersFactory;
import floq.test.experiments.metrics.IMetric;
import floq.Constants;
import floq.llm.models.TogetherAIModel;
import floq.llm.models.togetherai.TogetherAIConstants;
import floq.llm.query.INLQueryExectutor;
import floq.llm.query.IQueryExecutor;
import floq.llm.query.ISQLExecutor;
import floq.llm.query.utils.QueryUtils;
import floq.optimizer.IOptimizer;
import floq.optimizer.IndexedConditionPushdownOptimizer;
import floq.optimizer.NullOptimizer;
import floq.optimizer.QueryPlan;
import floq.optimizer.estimators.ConfidenceEstimator;
import floq.parser.ParserException;
import floq.parser.ParserFrom;
import floq.parser.ParserWhere;
import floq.test.experiments.Experiment;
import floq.test.model.ExpVariant;
import floq.utils.FLOQDebug;
import floq.utils.Mapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Key;
import engine.utility.EngineUtility;

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
                log.debug("NL Executor Prompt: {}", nlExecutor.getNaturalLanguagePrompt());
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
            FLOQDebug.log("*** Executing experiment " + experiment.toString() + " with variant: " + variant.toString() + " ***");
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
                if (optimizer != null) {
                    experiment.setOptimizers(List.of(optimizer));
                }
            }
            FLOQDebug.log("*** Executing experiment " + experiment.toString() + " with variant: " + variant.toString() + " ***");
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
    
    public Map<ITable, Map<Attribute, Double>> executeConfidenceEstimator(String path, ExpVariant variant) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());
            IDatabase database = experiment.getQuery().getDatabase();
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(experiment.getQuery().getSql());
            List<String> tables = parserFrom.getTables();
            ConfidenceEstimator estimator = new ConfidenceEstimator();
            Map<ITable, Map<Attribute, Double>> dbConfidence = estimator.getEstimation(database);
//            Map<ITable, Map<Attribute, Double>> dbConfidence = estimator.getEstimation(database, tables, experiment.getQuery().getSql());
            return dbConfidence;
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
//            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
        return null;
    }
    
    public Map<ITable, Double> executeConfidenceEstimatorSchema(String path, ExpVariant variant) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());
            IDatabase database = experiment.getQuery().getDatabase();
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(experiment.getQuery().getSql());
            List<String> tables = parserFrom.getTables();
            ConfidenceEstimator estimator = new ConfidenceEstimator();
            Map<ITable, Double> dbConfidence = estimator.getEstimationSchema(database);
//            Map<ITable, Map<Attribute, Double>> dbConfidence = estimator.getEstimation(database, tables, experiment.getQuery().getSql());
            return dbConfidence;
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
//            throw new RuntimeException("Cannot run experiment: " + path, ioe);
        }
        return null;
    }
    
    public void executeConfidenceEstimatorQuery(String path, ExpVariant variant) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());
            IDatabase database = experiment.getQuery().getDatabase();
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(experiment.getQuery().getSql());
            List<String> tables = parserFrom.getTables();
            ConfidenceEstimator estimator = new ConfidenceEstimator();
            String query = experiment.getQuery().getSql();
            int indexOfFrom = query.indexOf("FROM", 0);
            String queryFrom = query.substring(indexOfFrom).trim();
            query = "SELECT * " + queryFrom;
            query = query.replace("target.", "");
            estimator.getEstimationForQuery(database, tables, query);
//            Map<ITable, Map<Attribute, Double>> dbConfidence = estimator.getEstimation(database, tables, experiment.getQuery().getSql());
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
        }
    }
    
    public QueryPlan planEstimation(String path, ExpVariant variant) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());
            IDatabase database = experiment.getQuery().getDatabase();
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(experiment.getQuery().getSql());
            List<String> tables = parserFrom.getTables();
//            String query = experiment.getQuery().getSql();
//            int indexOfFrom = query.indexOf("FROM", 0);
//            String queryFrom = query.substring(indexOfFrom).trim();
//            query = "SELECT * " + queryFrom;
//            query = query.replace("target.", "");
            String query = experiment.getQuery().getSql();
            query = query.replace("target.", "");
            ConfidenceEstimator estimator = new ConfidenceEstimator();
            Map<String, String> estimation = estimator.getEstimationForQuery2(database, tables, query);
            ParserWhere parserWhere = new ParserWhere();
            parserWhere.parseWhere(experiment.getQuery().getSql());
            List<Expression> expressions = parserWhere.getExpressions();
            List<Expression> expressionPushDown = new ArrayList<>();
            int indexPushDown = 0;
            List<Integer> indexes = new ArrayList<>();
            for (Expression expression : expressions) {
                for (String attr : estimation.keySet()) {
                    if (estimation.get(attr).contains("high") && expression.toString().contains(attr)) { // TOOO add more values
                        expressionPushDown.add(expression);
                        indexes.add(indexPushDown);
                    }
                }
                indexPushDown++;
            }
            log.info("Pushdown: {}", expressionPushDown);
            Double confidence = estimator.getEstimationConfidence(database, tables, query, expressionPushDown);
            log.info("Confidence Keys: {}", confidence);
            QueryPlan plan = new QueryPlan(expressionPushDown, confidence, expressions.size(), indexes);
            return plan;
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
            return null;
        }
    }
    
    public void executeCardinalityEstimatorQuery(String path, ExpVariant variant) {
        try {
            log.info("*** Executing experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());
            log.debug("SQL query is {}", experiment.getQuery().getSql());
            IDatabase database = experiment.getQuery().getDatabase();
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(experiment.getQuery().getSql());
            List<String> tables = parserFrom.getTables();
            ConfidenceEstimator estimator = new ConfidenceEstimator();
            String query = experiment.getQuery().getSql();
            query = query.replace("target.", "");
            estimator.getCardinalityEstimationForQuery(database, tables, query);
//            Map<ITable, Map<Attribute, Double>> dbConfidence = estimator.getEstimation(database, tables, experiment.getQuery().getSql());
        } catch (Exception ioe) {
            log.error("Unable to execute experiment {}", path, ioe);
        }
    }

    public Double getPopularity(String expPath, ExpVariant variant) {
        Experiment experiment = null;
        try {
            experiment = ExperimentParser.loadAndParseJSON(expPath);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return -1.0;
        }
        IDatabase database = experiment.getQuery().getDatabase();
        ParserFrom parserFrom = new ParserFrom();
        parserFrom.parseFrom(variant.getQuerySql());
        List<String> tables = parserFrom.getTables();
        String prompt = "Given the following JSON schema:\n";
        Set<Key> allKeysForTable = new HashSet<>();
        for (String tableName : tables) {
            ITable table = database.getTable(tableName);
            List<Key> keys = database.getPrimaryKeys();
            List<Key> keysForTable = getKeysForTable(keys, table);
            allKeysForTable.addAll(keysForTable);
            String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
            prompt += jsonSchema + "\n";
        }
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(variant.getQuerySql());
        String whereExpression = parserWhere.getWhereExpression();
        prompt += "What is the popularity in your knowledge of " + toStringKeys(allKeysForTable) + " of " + EngineUtility.printCollection(tables);
        if (whereExpression != null && !whereExpression.trim().isEmpty()) {
            prompt += " where " + whereExpression;
        }
        prompt += "?\n";
        prompt += "Return a value between 0 and 1. Where 1 is very popular and 0 is not popular at all.\n"
                + "Respond with JSON only with a numerical property with name \"popularity\".";
        log.debug("Prompt Populatity: {}", prompt);
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_1_70B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        String cleanResponse = Mapper.toCleanJsonObject(response);
        Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        Double popularity = (Double) parsedResponse.getOrDefault("popularity", -1.0);
        log.debug("Popularity: {}", popularity);
        return popularity;
    }

    public IOptimizer getOptimizerBasedOnLLMOptimization(String expPath, ExpVariant variant) {
        Experiment experiment = null;
        try {
            experiment = ExperimentParser.loadAndParseJSON(expPath);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return null;
        }
        IOptimizer optimizer = null;
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(variant.getQuerySql());
        String whereExpression = parserWhere.getWhereExpression();
        if (whereExpression == null || whereExpression.isEmpty()) return null;
        IDatabase database = experiment.getQuery().getDatabase();
        ParserFrom parserFrom = new ParserFrom();
        parserFrom.parseFrom(variant.getQuerySql());
        List<String> tables = parserFrom.getTables();
        Map<String, DebugOptimizer> optimizerPerTable = new HashMap<>();
        String prompt = "You're an intellingent optimizer. Given a JSON schema, you will opt for a plan that will help you in return the highest accurare results.\n"
                + "\n"
                + "Your goal is to return the results related to the following JSON schema:\n";
        for (String tableName : tables) {
            Map<Integer, IOptimizer> optimizersPerTable = new HashMap<>();
            ITable table = database.getTable(tableName);
            List<Key> keys = database.getPrimaryKeys();
            List<Key> keysForTable = getKeysForTable(keys, table);
            HashSet<Key> keysInTable = new HashSet<>(keysForTable);
            String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
            prompt += jsonSchema + "\n\n";
            prompt += "You can opt on one of the following options to return the data:\n";
            //no optimization
            int index = 1;
            prompt += "option " + index + ") List all the " + tableName + " " + toStringKeyShort(keysInTable, tableName) + " and after filter the data\n";
            optimizersPerTable.put(index, new NullOptimizer());
            index++;
            prompt += "option " + index + ") List all the " + tableName + " " + toStringKeyShort(keysInTable, tableName) + " where " + whereExpression + "\n";
            optimizersPerTable.put(index, OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter"));
            List<Expression> expressions = parserWhere.getExpressions();
            if (expressions != null && expressions.size() > 1) {
                for (int i = 0; i < expressions.size(); i++) {
                    Expression expression = expressions.get(i);
                    index++;
                    prompt += "option " + index + ") List all the " + tableName + " " + toStringKeyShort(keysInTable, tableName) + " where " + expression.toString() + "\n";
                    IOptimizer singleConditionPushDownRemoveAlgebraTreeOptimizer = new IndexedConditionPushdownOptimizer(i, false);
                    optimizersPerTable.put(index, singleConditionPushDownRemoveAlgebraTreeOptimizer);
                }
            }
            prompt += "\n";
            prompt += "Then you will be asked to filter the data as a safe check.\n";
            prompt += "\n";
            prompt += "Choose a single option that returns the highest quality data. Answer only with the option number. Do not add any comment.";
            Integer option = getOptimizerNumber(prompt, index);
            IOptimizer optimizerForTable = optimizersPerTable.getOrDefault(option, null);
            optimizerPerTable.put(tableName, new DebugOptimizer(optimizerForTable, prompt));
        }
        // TODO we assume we return the best optimizer for the firstTable
        String tableName = tables.get(0);
        DebugOptimizer debugOptimizer = optimizerPerTable.get(tableName);
        if (debugOptimizer == null) {
            return null;
        }
        optimizer = debugOptimizer.getOptimizer();
        if (optimizer == null || optimizer instanceof NullOptimizer) {
            return null;
        }
        return optimizer;
    }

    public IOptimizer getOptimizerBasedOnCardinality(String expPath, ExpVariant variant) {
        Experiment experiment = null;
        try {
            experiment = ExperimentParser.loadAndParseJSON(expPath);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            return null;
        }
        IOptimizer optimizer = null;
        IDatabase database = experiment.getQuery().getDatabase();
        ParserFrom parserFrom = new ParserFrom();
        parserFrom.parseFrom(variant.getQuerySql());
        List<String> tables = parserFrom.getTables();
        String prompt = "Given the following JSON schema:\n";
        Map<String, Map<DebugOptimizer, Integer>> optimizersPerTable = new HashMap<>();
        for (String tableName : tables) {
            ITable table = database.getTable(tableName);
            List<Key> keys = database.getPrimaryKeys();
            List<Key> keysForTable = getKeysForTable(keys, table);
            ParserWhere parserWhere = new ParserWhere();
            parserWhere.parseWhere(variant.getQuerySql());
            String whereExpression = parserWhere.getWhereExpression();
            String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
            prompt += jsonSchema + "\n";
            prompt += "Estimate the cardinality of " + keysForTable.toString() + " of " + tableName;
            String promptAllConditionPushDown = prompt;
            List<String> promptSinglePushDown = new ArrayList<>();
            for (Expression expression : parserWhere.getExpressions()) {
                String singleCondition = prompt + " where " + expression.toString();
                promptSinglePushDown.add(singleCondition);
            }
            if (whereExpression != null && !whereExpression.trim().isEmpty()) {
                promptAllConditionPushDown += " where " + whereExpression;
            }
            String endPrompt = "\n";
            endPrompt += """
                          Respond with JSON only with a numerical property with name "cardinality".""";
            String promptNoPushDown = prompt + endPrompt;
            promptAllConditionPushDown += endPrompt;
            List<String> promptSingleConditionPushDown = new ArrayList<>();
            for (String single : promptSinglePushDown) {
                promptSingleConditionPushDown.add(single + endPrompt);
            }
            Map<DebugOptimizer, Integer> tableOptimizers = new HashMap<>();
            Integer cardinalityNoPushDown = getCardinality(promptNoPushDown);
            DebugOptimizer nullOptimizer = new DebugOptimizer(new NullOptimizer(), promptNoPushDown);
            tableOptimizers.put(nullOptimizer, cardinalityNoPushDown);
            if (whereExpression != null && !whereExpression.trim().isEmpty()) {
                Integer cardinalityAllConditionPushDown = getCardinality(promptAllConditionPushDown);
                IOptimizer allConditionPushdown = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer");
                DebugOptimizer allConditionPushdownOptimizer = new DebugOptimizer(allConditionPushdown, promptAllConditionPushDown);
                tableOptimizers.put(allConditionPushdownOptimizer, cardinalityAllConditionPushDown);
                if (parserWhere.getExpressions() != null && parserWhere.getExpressions().size() > 1) {
                    for (int i = 0; i < promptSingleConditionPushDown.size(); i++) {
                        String singleConditionPrompt = promptSingleConditionPushDown.get(i);
                        Integer cardinalitySingleCondition = getCardinality(singleConditionPrompt);
                        IOptimizer singleConditionPushDownRemoveAlgebraTreeOptimizer = new IndexedConditionPushdownOptimizer(i, true);
                        DebugOptimizer singleConditionPushDown = new DebugOptimizer(singleConditionPushDownRemoveAlgebraTreeOptimizer, singleConditionPrompt);
                        tableOptimizers.put(singleConditionPushDown, cardinalitySingleCondition);
                    }
                }
            }
            optimizersPerTable.put(tableName, tableOptimizers);
        }
//        log.info(EngineUtility.printMap(optimizersPerTable));
        // TODO we assume we return the best optimizer for the firstTable
        String tableName = tables.get(0);
        optimizer = getBestOptimizer(tableName, optimizersPerTable);
        return optimizer;
    }

    private List<Key> getKeysForTable(List<Key> keys, ITable table) {
        List<Key> keysForTable = new ArrayList<>();
        for (Key key : keys) {
            if (attributeNames(table.getAttributes()).containsAll(attributeRefNames(key.getAttributes()))) {
                keysForTable.add(key);
            }
        }
        return keysForTable;
    }

    private List<String> attributeNames(List<Attribute> attributes) {
        Set<String> names = new HashSet<>();
        for (Attribute a : attributes) {
            names.add(a.getName());
        }
        return new ArrayList<>(names);
    }

    private List<String> attributeRefNames(List<AttributeRef> attributes) {
        Set<String> names = new HashSet<>();
        for (AttributeRef a : attributes) {
            names.add(a.getName());
        }
        return new ArrayList<>(names);
    }

    private Integer getCardinality(String prompt) {
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_1_70B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        log.debug("Cardinality prompt: " + prompt);
        log.debug("Cardinality response: " + response);
        Map<String, Object> parsedResponse = new HashMap<>();
        try {
            String cleanResponse = Mapper.toCleanJsonObject(response);
            parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        } catch (Exception e) {
            // do nothing
        }
        Integer cardinality = (Integer) parsedResponse.getOrDefault("cardinality", Integer.MAX_VALUE);
        return cardinality;
    }

    private Integer getOptimizerNumber(String prompt, int maxIndex) {
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_1_70B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        log.debug("LLMOptimization prompt: " + prompt);
        log.debug("LLMOptimization response: " + response);
        try {
            Integer parsed = Integer.valueOf(response.trim());
            if (parsed >= 1 && parsed <= maxIndex) {
                return parsed;
            }
        } catch (NumberFormatException nfe) {
            return null;
        }
        return null;
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

    private String toStringKeys(Set<Key> keys) {
        StringBuilder sb = new StringBuilder();
        for (Key key : keys) {
            sb.append(key).append(", ");
        }
        String sReturn = sb.toString();
        log.debug("Keys To String: " + sReturn);
        return sReturn.substring(0, sReturn.length() - 2).trim();
    }
    
    private String toStringKeyShort(Set<Key> keys, String tableName) {
        StringBuilder sb = new StringBuilder();
        for (Key key : keys) {
            sb.append(key.getAttributes().toString().replace(tableName+".", "")
                    .replace("[", "")
                    .replace("]", "")).append(", ");
        }
        String sReturn = sb.toString();
        log.debug("Keys To String: " + sReturn);
        return sReturn.substring(0, sReturn.length() - 2).trim();
    }

    private IOptimizer getBestOptimizer(String tableName, Map<String, Map<DebugOptimizer, Integer>> optimizersPerTable) {
        Map<DebugOptimizer, Integer> optimizersForTable = optimizersPerTable.get(tableName);
        if (optimizersForTable == null || optimizersForTable.isEmpty()) {
            return null;
        }
        IOptimizer minOptimizer = null;
        Integer minCardinality = null;
        for (DebugOptimizer debugOptimizer : optimizersForTable.keySet()) {
            if (minCardinality == null) {
                minOptimizer = debugOptimizer.getOptimizer();
                minCardinality = optimizersForTable.get(debugOptimizer);
                continue;
            }
            if (optimizersForTable.get(debugOptimizer) < minCardinality) {
                minOptimizer = debugOptimizer.getOptimizer();
                minCardinality = optimizersForTable.get(debugOptimizer);
            }
        }
        if (minOptimizer instanceof NullOptimizer) {
            return null;
        }
        return minOptimizer;
    }

    private class DebugOptimizer {

        private IOptimizer optimizer;
        private String prompt;

        public DebugOptimizer(IOptimizer optimizer, String prompt) {
            this.optimizer = optimizer;
            this.prompt = prompt;
        }

        public IOptimizer getOptimizer() {
            return optimizer;
        }

        public String getPrompt() {
            return prompt;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.optimizer);
            hash = 29 * hash + Objects.hashCode(this.prompt);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DebugOptimizer other = (DebugOptimizer) obj;
            if (!Objects.equals(this.prompt, other.prompt)) {
                return false;
            }
            return Objects.equals(this.optimizer, other.optimizer);
        }

        @Override
        public String toString() {
            return "DebugOptimizer{" + "optimizer=" + optimizer + ", prompt=" + prompt + '}';
        }
    }
}
