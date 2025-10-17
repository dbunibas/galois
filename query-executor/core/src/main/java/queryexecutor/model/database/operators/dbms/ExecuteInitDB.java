package queryexecutor.model.database.operators.dbms;

import java.util.Collections;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DAOException;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.InitDBConfiguration;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.OperatorFactory;
import queryexecutor.model.algebra.operators.IBatchInsert;
import queryexecutor.model.algebra.operators.ICreateTable;
import queryexecutor.model.thread.IBackgroundThread;
import queryexecutor.model.thread.ThreadManager;
import queryexecutor.persistence.file.CSVFile;
import queryexecutor.persistence.file.IImportFile;
import queryexecutor.persistence.file.XMLFile;
import queryexecutor.persistence.file.operators.ImportCSVFile;
import queryexecutor.persistence.file.operators.ImportCSVFileWithCopy;
import queryexecutor.persistence.file.operators.ImportXMLFile;
import queryexecutor.utility.DBMSUtility;

public class ExecuteInitDB {

    private static Logger logger = LoggerFactory.getLogger(ExecuteInitDB.class);
    private ImportCSVFile csvFileImporter;
    private ImportCSVFileWithCopy csvFileImporterWithCopy;
    private ImportXMLFile xmlFileImporter;
    private ICreateTable tableCreator;
    private IBatchInsert batchInsertOperator;
    private IValueEncoder valueEncoder;

    public void execute(DBMSDB db) {
        initOperators(db);
        InitDBConfiguration configuration = db.getInitDBConfiguration();
        valueEncoder = configuration.getValueEncoder();
        csvFileImporter = new ImportCSVFile(tableCreator, batchInsertOperator, valueEncoder);
        csvFileImporterWithCopy = new ImportCSVFileWithCopy(tableCreator, valueEncoder);
        xmlFileImporter = new ImportXMLFile(tableCreator, batchInsertOperator, valueEncoder);
        if (logger.isDebugEnabled()) logger.debug("Initializating DB with configuration " + configuration);
        AccessConfiguration accessConfiguration = db.getAccessConfiguration();
        if (configuration.getInitDBScript() == null && configuration.hasFilesToImport() && !DBMSUtility.isSchemaExists(accessConfiguration)
                && DBMSUtility.supportsSchema(accessConfiguration)) {
            configuration.setInitDBScript(createSchemaScript(accessConfiguration.getSchemaAndSuffix()));
        }
        if (configuration.getInitDBScript() != null) {
            QueryManager.executeScript(configuration.getInitDBScript(), accessConfiguration, false, true, false, false);
        }
        if (configuration.hasFilesToImport()) {
            importFiles(db);
        }
        if (configuration.getPostDBScript() != null) {
            QueryManager.executeScript(configuration.getPostDBScript(), accessConfiguration, false, true, false, false);
        }
    }

    private void importFiles(DBMSDB db) {
        InitDBConfiguration configuration = db.getInitDBConfiguration();
        int numOfThreads = configuration.getNumOfThreads();
//        numOfThreads = Math.min(numOfThreads, 5);
        assert (numOfThreads > 0);
        ThreadManager threadManager = new ThreadManager(numOfThreads);
        Map<String, List<Attribute>> tablesAdded = Collections.synchronizedMap(new HashMap<String, List<Attribute>>());
        for (String tableName : configuration.getTablesToImport()) {
            for (IImportFile fileToImport : configuration.getFilesToImport(tableName)) {
                ImportFileThread execThread = new ImportFileThread(configuration, fileToImport, tableName, db, tablesAdded);
                threadManager.startThread(execThread);
            }
        }
        threadManager.waitForActiveThread();
    }

    class ImportFileThread implements IBackgroundThread {

        private InitDBConfiguration configuration;
        private IImportFile fileToImport;
        private String tableName;
        private DBMSDB db;
        private Map<String, List<Attribute>> tablesAdded;

        public ImportFileThread(InitDBConfiguration configuration, IImportFile fileToImport, String tableName, DBMSDB db, Map<String, List<Attribute>> tablesAdded) {
            this.configuration = configuration;
            this.fileToImport = fileToImport;
            this.tableName = tableName;
            this.db = db;
            this.tablesAdded = tablesAdded;
        }

        public void execute() {
            if (logger.isDebugEnabled()) logger.debug("Importing file " + fileToImport.getFileName() + " into table " + tableName);
            if (fileToImport.getType().equals(QueryExecutorConstants.XML)) {
                xmlFileImporter.importXMLFile(tableName, (XMLFile) fileToImport, tablesAdded, db);
            } else if (fileToImport.getType().equals(QueryExecutorConstants.CSV) && configuration.isUseCopyStatement()) {
                csvFileImporterWithCopy.importCSVFile(tableName, (CSVFile) fileToImport, tablesAdded, db);
            } else if (fileToImport.getType().equals(QueryExecutorConstants.CSV) && !configuration.isUseCopyStatement()) {
                csvFileImporter.importCSVFile(tableName, (CSVFile) fileToImport, tablesAdded, db);
            } else {
                throw new DAOException("Unsupported file: " + fileToImport.getType());
            }
        }

    }

    private String createSchemaScript(String schemaName) {
        if (schemaName.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("create schema ").append(schemaName).append(";\n");
        return sb.toString();
    }

    private void initOperators(DBMSDB database) {
        this.tableCreator = OperatorFactory.getInstance().getTableCreator(database);
        this.batchInsertOperator = OperatorFactory.getInstance().getSingletonBatchInsertOperator(database);
    }

}
