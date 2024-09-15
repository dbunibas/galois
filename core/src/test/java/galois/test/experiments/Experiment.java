package galois.test.experiments;

import com.galois.sqlparser.SQLQueryParser;
import com.galois.sqlparser.ScanNodeFactory;
import com.galois.sqlparser.TableAliasQueryParser;
import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.LLMQueryStatManager;
import galois.optimizer.IOptimizer;
import galois.optimizer.LogicalPlanOptimizer;
import galois.parser.ParserWhere;
import galois.test.experiments.metrics.IMetric;
import galois.test.utils.TestUtils;
import galois.utils.GaloisDebug;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import speedy.exceptions.DAOException;
import speedy.exceptions.DBMSException;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Tuple;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.persistence.DAODBMSDatabase;
import speedy.persistence.Types;
import speedy.persistence.file.CSVFile;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.util.*;
import net.sf.jsqlparser.expression.Expression;

@AllArgsConstructor
@Data
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public final class Experiment {

    @ToString.Include
    private String name;
    private final String dbms;
    private final List<IMetric> metrics;
    @ToString.Include
    private List<IOptimizer> optimizers;
    private final OperatorsConfiguration operatorsConfiguration;
    private final Query query;
    private final String queryExecutor;

    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> execute() {
        Map<String, ExperimentResults> results = new HashMap<>();
        // TODO: Make this generic (and remove annotation)
//        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
//        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);
//        Document queryPlan = planner.planFrom(query.getSql());
//        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration, query.getSql());
        IAlgebraOperator operator = parse();
        log.info("Query operator {}", operator);
        DBMSDB dbmsDatabase = createDatabaseForExpected();
        String queryToExecute = query.getSql().replace("target.", "public.");
        log.debug("Query for results: \n" + queryToExecute);
        ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
        ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
        List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
        expectedITerator.close();
        log.info("Expected size: " + expectedResults.size());
//        log.info("Expected");
//        log.info("Query: " + query.getSql());
//        for (Tuple expectedResult : expectedResults) {
//            log.info(expectedResult.toString());
//        }
//        log.info("-----------------------------");

        GaloisDebug.log("Unoptimized");
        var unoptimizedResult = executeUnoptimizedExperiment(operator, expectedResults);
        GaloisDebug.log("Speedy Results:");
        GaloisDebug.log(unoptimizedResult.toDebugString());
        results.put("Unoptimized", unoptimizedResult);

        List<IOptimizer> optimizersList = optimizers == null ? List.of() : optimizers;
        for (IOptimizer optimizer : optimizersList) {
            GaloisDebug.log(optimizer.getName());
            var result = executeOptimizedExperiment(operator, optimizer, expectedResults);
            GaloisDebug.log("Speedy Results:");
            GaloisDebug.log(result.toDebugString());
            results.put(optimizer.getName(), result);
        }
        return results;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> executeGalois(Map<ITable, Map<Attribute, Double>> dbConfidence, double threshold, boolean removeFromAlgebraTree) {
        Map<String, ExperimentResults> results = new HashMap<>();
        IAlgebraOperator operator = parse();
        String sqlQuery = query.getSql();
        IDatabase database = query.getDatabase();
        LogicalPlanOptimizer optimizer = new LogicalPlanOptimizer();
        operator = optimizer.optimizeByConfidence(operator, sqlQuery, database, dbConfidence, threshold, removeFromAlgebraTree);
        String optimizations = optimizer.getOptimizations();
        log.info("Query operator {}", operator);
        DBMSDB dbmsDatabase = createDatabaseForExpected();
        String queryToExecute = query.getSql().replace("target.", "public.");
        log.debug("Query for results: \n" + queryToExecute);
        ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
        ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
        List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
        expectedITerator.close();
        log.info("Expected size: " + expectedResults.size());
        GaloisDebug.log("Galois Execution");
        var galoisResults = executeUnoptimizedExperiment(operator, expectedResults);
        GaloisDebug.log("Speedy Results:");
        GaloisDebug.log(galoisResults.toDebugString());
        String name = "Galois";
        if (!optimizations.isEmpty()) name += " - " + optimizations;
        results.put(name, galoisResults);
        return results;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> executeSingle(IOptimizer optimizer) {
        Map<String, ExperimentResults> results = new HashMap<>();
        IAlgebraOperator operator = parse();
        log.info("Query operator {}", operator);
        DBMSDB dbmsDatabase = createDatabaseForExpected();
        String queryToExecute = query.getSql().replace("target.", "public.");
        log.debug("Query for results: \n" + queryToExecute);
        ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
        ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
        List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
        expectedITerator.close();
        log.info("Expected size: " + expectedResults.size());
        if (optimizer == null) {
            GaloisDebug.log("Unoptimized");
            var unoptimizedResult = executeUnoptimizedExperiment(operator, expectedResults);
            GaloisDebug.log("Speedy Results:");
            GaloisDebug.log(unoptimizedResult.toDebugString());
            results.put("Unoptimized", unoptimizedResult);
        } else {
            GaloisDebug.log(optimizer.getName());
            var result = executeOptimizedExperiment(operator, optimizer, expectedResults);
            GaloisDebug.log("Speedy Results:");
            GaloisDebug.log(result.toDebugString());
            results.put(optimizer.getName(), result);
        }
        return results;
    }

    public IAlgebraOperator parse() {
//        Previous strategy: use PostgreSQL query plan parser
//        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
//        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);
//        Document queryPlan = planner.planFrom(query.getSql());
//        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration, query.getSql());
//        return operator;
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        log.info("Parsing the query using SQLQueryParser - {}", sqlQueryParser.getClass());
        IQueryExecutor scanQueryExecutor = operatorsConfiguration.getScan().getQueryExecutor();
        String normalizationStrategy = operatorsConfiguration.getScan().getNormalizationStrategy();
        ScanNodeFactory scanNodeFactory = (tableAlias, attributes) -> new LLMScan(tableAlias, operatorsConfiguration.getScan().createQueryExecutor(), attributes, normalizationStrategy);

        // If ignoreTree() returns true, only execute the LLMScan operation
        if (scanQueryExecutor.ignoreTree()) {
            TableAliasQueryParser tableAliasQueryParser = new TableAliasQueryParser();
            return scanNodeFactory.createScanNode(tableAliasQueryParser.parse(query.getSql()), null);
        }

        return sqlQueryParser.parse(query.getSql(), scanNodeFactory);
    }

    private DBMSDB createDatabaseForExpected() throws IllegalStateException, DAOException {
        IDatabase database = query.getDatabase();
        List<String> tableNames = database.getTableNames();
        URL resource = Experiment.class.getResource(query.getResultPath());
        File[] files = new File(resource.getPath()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv") && !name.contains("_speedy");
            }
        });
        Map<String, File> tableCSVFiles = new HashMap<>();
        for (File file : files) {
            String tableName = file.getName().replace(".csv", "");
            log.debug("Table: " + tableName + " --- " + file);
            tableCSVFiles.put(tableName, file);
        }
        DBMSDB dbmsDatabase = null;
        DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
        dbmsDatabase = daoDatabase.loadDatabase(query.getAccessConfiguration().getDriver(),
                query.getAccessConfiguration().getUri(),
                "public",
                query.getAccessConfiguration().getLogin(),
                query.getAccessConfiguration().getPassword());
        dbmsDatabase.getInitDBConfiguration().setCreateTablesFromFiles(true);
        List<File> speedyFiles = new ArrayList<>();
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            if (table.getSize() == 0) {
                // import table
                File file = tableCSVFiles.get(tableName);
                log.debug("Search for table: " + table + " found file: " + file);
                File speedyFile = new File(resource.getPath() + File.separator + file.getName().replace(".csv", "") + "_speedy.csv");
                String textDelim = null;
                try {
                    FileUtils.copyFile(file, speedyFile);
                    textDelim = replaceHeadersWithTypes(speedyFile, attributes);
                } catch (IOException ioe) {
                    log.error("Unable to duplicate file: " + file + " to " + speedyFile);
                    log.error("Exception: " + ioe);
                    throw new IllegalStateException(ioe);
                }
                CSVFile fileToImport = new CSVFile(speedyFile.getAbsolutePath());
                System.out.println("File to import: " + speedyFile.getAbsolutePath());
                speedyFiles.add(speedyFile);
                fileToImport.setHasHeader(true);
                fileToImport.setSeparator(',');
                if (textDelim != null) {
                    log.debug("Text Delim: " + textDelim);
                    fileToImport.setQuoteCharacter(textDelim.charAt(0));
                }
                dbmsDatabase.getInitDBConfiguration().addFileToImportForTable(tableName, fileToImport);
            }
        }
        dbmsDatabase.initDBMS();
        for (File speedyFile : speedyFiles) {
            try {
                FileUtils.delete(speedyFile);
            } catch (IOException ioe) {
                log.error("Unable to delete: " + speedyFile);
            }
        }
        return dbmsDatabase;
    }

    private ExperimentResults executeUnoptimizedExperiment(IAlgebraOperator operator, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        ITupleIterator iterator = operator.execute(query.getDatabase(), query.getDatabase());
        return toExperimentResults(iterator, expectedResults, null);
    }

    private ExperimentResults executeOptimizedExperiment(IAlgebraOperator operator, IOptimizer optimizer, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        IAlgebraOperator optimizedOperator = optimizer.optimize(query.getDatabase(), query.getSql(), operator);
        log.info("Optimized operator: {}", optimizedOperator);
        ITupleIterator iterator = optimizedOperator.execute(query.getDatabase(), query.getDatabase());
        return toExperimentResults(iterator, expectedResults, optimizer.getName());
    }

    private ExperimentResults toExperimentResults(ITupleIterator actual, List<Tuple> expectedResults, String optmizerName) {
        List<Tuple> results = TestUtils.toTupleList(actual);
        log.info("Results size: " + results.size());
        log.info("Results: " + results);
        List<Double> scores = metrics
                .stream()
                //                .map(m -> m.getScore(query.getDatabase(), query.getResults(), results))
                .map(m -> m.getScore(query.getDatabase(), expectedResults, results))
                .toList();

        // TODO [Stats]: Add stats to results
//        return new ExperimentResults(name, metrics, query.getResults(), results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql());
        return new ExperimentResults(name, metrics, expectedResults, results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql(), optmizerName);
    }

    public static void dropTable(DBMSDB database) {
        AccessConfiguration accessConfiguration = database.getAccessConfiguration();
        String dbName = database.getName() + "." + database.getFirstTable().getName();
        try {
//            log.info("Removing db " + accessConfiguration.getDatabaseName() + ", if exist...");
            String script = "DROP TABLE " + dbName + ";";
            log.info("Script drop: " + script);
            QueryManager.executeScript(script, accessConfiguration, true, true, true, true);
//            log.info("Database removed!");
        } catch (DBMSException ex) {
            log.warn("Unable to drop table " + dbName + ". " + ex.getLocalizedMessage());
        }
    }

    private String replaceHeadersWithTypes(File speedyFile, List<Attribute> attributes) throws IOException {
        List<String> lines = FileUtils.readLines(speedyFile, "utf-8");
        String headers = lines.get(0);
        StringTokenizer tokenizer = new StringTokenizer(headers, ",");
        String textDelim = getTextDelim(headers);
        String updatedHeaders = updateHeaders(tokenizer, attributes, textDelim);
        lines.remove(0);
        lines.add(0, updatedHeaders);
        FileUtils.writeLines(speedyFile, lines);
        return textDelim; // TODO refactor
    }

    private String updateHeaders(StringTokenizer tokenizer, List<Attribute> attributes, String textDelim) {
        Map<String, String> attributesWithType = new HashMap<>();
        for (Attribute attribute : attributes) {
            attributesWithType.put(attribute.getName(), attribute.getType());
        }
        String headersWithType = "";
        while (tokenizer.hasMoreTokens()) {
            String attributeName = tokenizer.nextToken().trim();
//            String attributeNameCleaned = attributeName.replace("\"", "");
            if (textDelim != null) {
                attributeName = attributeName.replace(textDelim, "");
            }
            String type = attributesWithType.get(attributeName);
            if (type == null || type.equals(Types.STRING)) {
                headersWithType += attributeName + ",";
            } else {
                if (textDelim == null) {
                    headersWithType += attributeName + "(" + type + ")" + ",";
                } else {
                    headersWithType += textDelim + attributeName + "(" + type + ")" + textDelim + ",";
                }
            }
        }
        headersWithType = headersWithType.substring(0, headersWithType.length() - 1);
        return headersWithType;
    }

    private String getTextDelim(String headers) {
        if (headers.contains("\"")) {
            return "\"";
        }
        if (headers.contains("'")) {
            return "'";
        }
        return null;
    }

}
