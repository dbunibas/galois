package engine.model.database.operators.lazyloading;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import engine.exceptions.DBMSException;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;

public class DBMSTupleLoaderIterator implements  Iterator<ITupleLoader>{

    private ResultSet resultSet;
    private String tableName;
    private boolean empty;
    private boolean firstTupleRead;
    private final AccessConfiguration accessConfiguration;
    private final String virtualTableName;

    public DBMSTupleLoaderIterator(ResultSet resultSet, String tableName, AccessConfiguration configuration) {
        this(resultSet, tableName, tableName, configuration);
    }

    public DBMSTupleLoaderIterator(ResultSet resultSet, String tableName, String virtualTableName, AccessConfiguration configuration) {
        this.resultSet = resultSet;
        this.tableName = tableName;
        this.virtualTableName = virtualTableName;
        this.accessConfiguration = configuration;
        try {
            firstTupleRead = resultSet.next();
            if (!firstTupleRead) {
                empty = true;
            }
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public boolean hasNext() {
        boolean next = false;    
        try {
            if (firstTupleRead) {
                next= true;
            }else if ( resultSet.isLast()){
                QueryManager.closeResultSet(resultSet);
            }else{
                next = true;
            }
            return next;
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public DBMSTupleLoader next() {
        try {
            if (firstTupleRead) {
                firstTupleRead = false;
            } else {
                resultSet.next();
            }
            DBMSTupleLoader tupleLoader = DBMSUtility.createTupleLoader(resultSet, tableName, virtualTableName, accessConfiguration);
            return tupleLoader;
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

  
}