package galois.test.experiments;

import galois.Constants;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.query.LLMQueryStatManager;
import galois.optimizer.IOptimizer;
import galois.parser.IQueryPlanParser;
import galois.planner.IQueryPlanner;
import galois.test.experiments.metrics.IMetric;
import galois.test.utils.TestUtils;
import galois.utils.excelreport.ReportExcel;
import galois.utils.excelreport.ReportRow;
import galois.utils.excelreport.SheetReport;
import galois.utils.excelreport.persistance.DAOReportExcel;
import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import speedy.OperatorFactory;
import speedy.exceptions.DAOException;
import speedy.exceptions.DBMSException;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.persistence.DAODBMSDatabase;
import speedy.persistence.Types;
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
    private boolean exportExcel = true;

    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> execute() {
        Map<String, ExperimentResults> results = new HashMap<>();
        // TODO: Make this generic (and remove annotation)
        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);
        Document queryPlan = planner.planFrom(query.getSql());
        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration, query.getSql());
        DBMSDB dbmsDatabase = createDatabaseForExpected();
        String queryToExecute = query.getSql().replace("target.", "public.");
        log.debug("Query for results: \n" + queryToExecute);
        ResultSet resultSet = QueryManager.executeQuery(queryToExecute, dbmsDatabase.getAccessConfiguration());
        ITupleIterator expectedITerator = new DBMSTupleIterator(resultSet);
        List<Tuple> expectedResults = TestUtils.toTupleList(expectedITerator);
        log.info("Expected size: " + expectedResults.size());
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
        if (exportExcel) {
            String pathExport = Constants.EXPORT_EXCEL_PATH;
            String fileName = pathExport + this.name + ".xlsx";
            exportExcel(fileName, results);
        }
        return results;
    }

    private DBMSDB createDatabaseForExpected() throws IllegalStateException, DAOException {
        ITable firstTable = query.getDatabase().getFirstTable();
        DBMSDB dbmsDatabase = null;
        List<Attribute> attributes = firstTable.getAttributes();
        if (firstTable.getSize() == 0) {
            URL resource = Experiment.class.getResource(query.getResultPath());
            File[] files = new File(resource.getPath()).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".csv") && !name.contains("_speedy");
                }
            });

            File file = files[0];
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
            DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
            dbmsDatabase = daoDatabase.loadDatabase(query.getAccessConfiguration().getDriver(),
                    query.getAccessConfiguration().getUri(),
                    "public",
                    query.getAccessConfiguration().getLogin(),
                    query.getAccessConfiguration().getPassword());
            dbmsDatabase.getInitDBConfiguration().setCreateTablesFromFiles(true);
            CSVFile fileToImport = new CSVFile(speedyFile.getAbsolutePath());
            System.out.println("File to import: " + speedyFile.getAbsolutePath());
            fileToImport.setHasHeader(true);
            fileToImport.setSeparator(',');
            if (textDelim != null) {
                fileToImport.setQuoteCharacter(textDelim.charAt(0));
            }
            dbmsDatabase.getInitDBConfiguration().addFileToImportForTable(firstTable.getName(), fileToImport);
            dbmsDatabase.initDBMS();
            OperatorFactory.getInstance().getQueryRunner(dbmsDatabase);
            if (speedyFile != null) {
                try {
                    FileUtils.delete(speedyFile);
                } catch (IOException ioe) {
                    log.error("Unable to delete: " + speedyFile);
                }
            }
        }
        return dbmsDatabase;
    }

    private ExperimentResults executeUnoptimizedExperiment(IAlgebraOperator operator, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        ITupleIterator iterator = operator.execute(query.getDatabase(), null);
        return toExperimentResults(iterator, expectedResults, null);
    }

    private ExperimentResults executeOptimizedExperiment(IAlgebraOperator operator, IOptimizer optimizer, List<Tuple> expectedResults) {
        // TODO [Stats]: Reset stats
        LLMQueryStatManager.getInstance().resetStats();
        IAlgebraOperator optimizedOperator = optimizer.optimize(query.getDatabase(), query.getSql(), operator);
        ITupleIterator iterator = optimizedOperator.execute(query.getDatabase(), null);
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

    private void exportExcel(String fileName, Map<String, ExperimentResults> results) {
        if (results.isEmpty()) {
            return;
        }
        DAOReportExcel daoReportExcel = new DAOReportExcel();
        ReportExcel reportExcel = new ReportExcel(this.name);
        List<String> optmizers = new ArrayList<>(results.keySet());
        Collections.sort(optmizers);
        SheetReport dataSheet = reportExcel.addSheet(this.name);
        createHeaders(optmizers, dataSheet);
        for (IMetric metric : metrics) {
            ReportRow rowMetric = dataSheet.addRow();
            rowMetric.addCell(metric.getName());
            for (String optmizer : optmizers) {
                ExperimentResults expResult = results.get(optmizer);
                Double value = expResult.getMetrics().get(metric.getName());
                if (value.isNaN()) value = 0.0;
                rowMetric.addCell(new DecimalFormat("#.###").format(value));
            }
        }
        addLLMStats(optmizers, results, dataSheet);       
        File exportFile = new File(fileName);
        log.info("Writing file {}", exportFile);
        daoReportExcel.saveReport(reportExcel, exportFile);
        try {
            Desktop.getDesktop().open(exportFile);
        } catch (IOException e) {
        }
    }

    private void createHeaders(List<String> optmizers, SheetReport dataSheet) {
        ReportRow rowHeader = dataSheet.addRow();
        rowHeader.addCell("");
        for (String optmizer : optmizers) {
            rowHeader.addCell(optmizer);
        }
    }

    private void addLLMStats(List<String> optmizers, Map<String, ExperimentResults> results, SheetReport dataSheet) {
        ReportRow rowTotalRequest = dataSheet.addRow();
        rowTotalRequest.addCell("LLM Total Requests");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            rowTotalRequest.addCell(expResult.getLlmRequest());
        }
        ReportRow towTotalInputTokens = dataSheet.addRow();
        towTotalInputTokens.addCell("LLM Total Input Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalInputTokens.addCell(expResult.getLlmTokensInput());
        }
        ReportRow towTotalOutputTokens = dataSheet.addRow();
        towTotalOutputTokens.addCell("LLM Total Output Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalOutputTokens.addCell(expResult.getLlmTokensOutput());
        }
        ReportRow towTotalTokens = dataSheet.addRow();
        towTotalTokens.addCell("LLM Total Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalTokens.addCell(expResult.getLlmTokensInput() + expResult.getLlmTokensOutput());
        }
        ReportRow towTotalTime = dataSheet.addRow();
        towTotalTime.addCell("LLM Time (ms)");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalTime.addCell(expResult.getTimeMs());
        }
    }
}
