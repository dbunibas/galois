package engine.model.algebra.operators.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.operators.IBatchInsert;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.dbms.DBMSTable;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;

public class SQLBatchInsert implements IBatchInsert {

    private static Logger logger = LoggerFactory.getLogger(SQLBatchInsert.class);
    private Lock lock = new java.util.concurrent.locks.ReentrantLock();
    private Map<ITable, List<Tuple>> buffer = new HashMap<ITable, List<Tuple>>();
    private SQLInsertTuple insertTupleOperator = new SQLInsertTuple();
    private int BUFFER_SIZE_POSTGRES = 10000;
    private int BUFFER_SIZE_MYSQL = 1000;

    public void insert(ITable table, Tuple tuple, IDatabase database) {
        if (logger.isDebugEnabled()) logger.debug("Trying to get lock on inserts... ");
        lock.lock();
        try {
            if (logger.isDebugEnabled()) logger.debug("Inserting tuple: " + tuple);
            List<Tuple> tuplesForTable = getTuplesForTable(table);
            tuplesForTable.add(tuple);
            if (tuplesForTable.size() > getBufferSize(((DBMSDB) database).getAccessConfiguration())) {
                insertTuples(table, tuplesForTable, database);
                buffer.remove(table);
            }
        } finally {
            lock.unlock();
        }
    }

    public void flush(IDatabase database) {
        lock.lock();
        try {
            if (logger.isDebugEnabled()) logger.debug("Flushing tuples...");
            for (ITable table : buffer.keySet()) {
                List<Tuple> tuplesForTable = getTuplesForTable(table);
                insertTuples(table, tuplesForTable, database);
            }
            buffer.clear();
        } finally {
            lock.unlock();
        }
    }

    private List<Tuple> getTuplesForTable(ITable table) {
        List<Tuple> result = buffer.get(table);
        if (result == null) {
            result = new ArrayList<Tuple>();
            buffer.put(table, result);
        }
        return result;
    }

    private void insertTuples(ITable table, List<Tuple> tuplesForTable, IDatabase database) {
        String tableName = table.getName();
        AccessConfiguration accessConfiguration = ((DBMSDB) database).getAccessConfiguration();
        StringBuilder sb = new StringBuilder();
        for (Tuple tuple : tuplesForTable) {
            sb.append(insertTupleOperator.buildInsertScript((DBMSTable) table, tuple, null, database)).append(getStatementTermination(accessConfiguration));
        }
        if (logger.isDebugEnabled()) logger.debug(tuplesForTable.size() + " tuple inserted in table " + tableName);
        QueryManager.executeScript(sb.toString(), accessConfiguration, true, true, false, false);
    }

    private int getBufferSize(AccessConfiguration accessConfiguration) {
        if (DBMSUtility.isMySQL(accessConfiguration.getDriver())) {
            return BUFFER_SIZE_MYSQL;
        }
        return BUFFER_SIZE_POSTGRES;
    }

    private String getStatementTermination(AccessConfiguration accessConfiguration) {
        if (DBMSUtility.isMySQL(accessConfiguration.getDriver())) {
            return "\n";
        }
        return "";
    }

}
