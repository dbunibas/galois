package engine.model.database.operators.dbms;

import engine.model.database.EmptyDB;
import engine.model.database.IDatabase;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.operators.IAnalyzeDatabase;
import engine.model.thread.IBackgroundThread;
import engine.model.thread.ThreadManager;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;

public class SQLAnalyzeDatabase implements IAnalyzeDatabase {

    public void analyze(IDatabase database, int maxNumberOfThreads) {
        if (database instanceof EmptyDB) {
            return;
        }
        ThreadManager threadManager = new ThreadManager(maxNumberOfThreads);
        for (String tableName : database.getTableNames()) {
            DBMSDB dbmsDB = (DBMSDB) database;
            AnalyzeTableThread analyzeThread = new AnalyzeTableThread(tableName, dbmsDB);
            threadManager.startThread(analyzeThread);
        }
        threadManager.waitForActiveThread();
    }

    class AnalyzeTableThread implements IBackgroundThread {

        private String tableName;
        private DBMSDB dbmsDB;

        public AnalyzeTableThread(String tableName, DBMSDB dbmsDB) {
            this.tableName = tableName;
            this.dbmsDB = dbmsDB;
        }

        public void execute() {
            AccessConfiguration accessConfiguration = dbmsDB.getAccessConfiguration();
            StringBuilder sb = new StringBuilder();
            sb.append("VACUUM ANALYZE ").append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(tableName).append(";\n");
            QueryManager.executeScript(sb.toString(), accessConfiguration, true, true, true, false);
        }

    }

}
