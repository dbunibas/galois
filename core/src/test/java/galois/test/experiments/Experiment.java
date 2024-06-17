package galois.test.experiments;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.query.LLMQueryStatManager;
import galois.optimizer.IOptimizer;
import galois.parser.IQueryPlanParser;
import galois.planner.IQueryPlanner;
import galois.test.experiments.metrics.IMetric;
import galois.test.utils.TestUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.sql.ResultSet;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import speedy.OperatorFactory;
import speedy.exceptions.DBMSException;
import speedy.model.database.ITable;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.persistence.DAODBMSDatabase;
import speedy.persistence.file.CSVFile;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;
import speedy.utility.DBMSUtility;

@Data
@Slf4j
public final class Experiment {

    private final String name;
    private final String dbms;
    private final List<IMetric> metrics;
    private final List<IOptimizer> optimizers;
    private final OperatorsConfiguration operatorsConfiguration;
    private final Query query;
    private final String queryExecutor;

    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> execute() {
        Map<String, ExperimentResults> results = new HashMap<>();

        // TODO: Make this generic (and remove annotation)
        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);

        Document queryPlan = planner.planFrom(query.getSql());
        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration);

        ITable firstTable = query.getDatabase().getFirstTable();
        DBMSDB dbmsDatabase = null;
        if (firstTable.getSize() == 0) {
            URL resource = Experiment.class.getResource(query.getResultPath());
            File[] files = new File(resource.getPath()).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".csv");
                }
            });
            File file = files[0];
            DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
            dbmsDatabase = daoDatabase.loadDatabase(query.getAccessConfiguration().getDriver(),
                    query.getAccessConfiguration().getUri(),
                    "public",
                    query.getAccessConfiguration().getLogin(),
                    query.getAccessConfiguration().getPassword());
            dbmsDatabase.getInitDBConfiguration().setCreateTablesFromFiles(true);
            CSVFile fileToImport = new CSVFile(file.getAbsolutePath());
            fileToImport.setHasHeader(true);
            fileToImport.setSeparator(',');
            dbmsDatabase.getInitDBConfiguration().addFileToImportForTable(firstTable.getName(), fileToImport);
            dbmsDatabase.initDBMS();
            OperatorFactory.getInstance().getQueryRunner(dbmsDatabase);
        }
        String queryToExecute = query.getSql().replace("target.", "public.");
        ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
        ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
        List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
//        log.info("Expected");
//        log.info("Query: " + query.getSql());
//        for (Tuple expectedResult : expectedResults) {
//            log.info(expectedResult.toString());
//        }
//        log.info("-----------------------------");

        var unoptimizedResult = executeUnoptimizedExperiment(operator, expectedResults);
        results.put("Unoptimized", unoptimizedResult);

        List<IOptimizer> optimizersList = optimizers == null ? List.of() : optimizers;
        for (IOptimizer optimizer : optimizersList) {
            var result = executeOptimizedExperiment(operator, optimizer, expectedResults);
            results.put(optimizer.getName(), result);
        }
        if (dbmsDatabase != null) {
            deleteDB(dbmsDatabase.getAccessConfiguration());
        }
        return results;
    }

    private ExperimentResults executeUnoptimizedExperiment(IAlgebraOperator operator, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        ITupleIterator iterator = operator.execute(query.getDatabase(), null);
        return toExperimentResults(iterator, expectedResults);
    }

    private ExperimentResults executeOptimizedExperiment(IAlgebraOperator operator, IOptimizer optimizer, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        IAlgebraOperator optimizedOperator = optimizer.optimize(query.getDatabase(), query.getSql(), operator);
        ITupleIterator iterator = optimizedOperator.execute(query.getDatabase(), null);
        return toExperimentResults(iterator, expectedResults);
    }

    private ExperimentResults toExperimentResults(ITupleIterator actual, List<Tuple> expectedResults) {
        List<Tuple> results = TestUtils.toTupleList(actual);
        List<Double> scores = metrics
                .stream()
                //                .map(m -> m.getScore(query.getDatabase(), query.getResults(), results))
                .map(m -> m.getScore(query.getDatabase(), expectedResults, results))
                .toList();

        // TODO [Stats]: Add stats to results
//        return new ExperimentResults(name, metrics, query.getResults(), results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql());
        return new ExperimentResults(name, metrics, expectedResults, results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql());
    }

    public static void deleteDB(AccessConfiguration accessConfiguration) {
        try {
//            log.info("Removing db " + accessConfiguration.getDatabaseName() + ", if exist...");
            DBMSUtility.deleteDB(accessConfiguration);
//            log.info("Database removed!");
        } catch (DBMSException ex) {
            String message = ex.getMessage();
            if (!message.contains("does not exist")) {
                log.warn("Unable to drop database.\n" + ex.getLocalizedMessage());
            }
        }
    }
}
