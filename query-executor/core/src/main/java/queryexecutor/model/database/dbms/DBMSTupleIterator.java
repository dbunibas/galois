package queryexecutor.model.database.dbms;

import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.Tuple;
import queryexecutor.exceptions.DBMSException;
import queryexecutor.utility.DBMSUtility;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.persistence.relational.QueryStatManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import queryexecutor.QueryExecutorConstants;

public class DBMSTupleIterator implements ITupleIterator {

    private ResultSet resultSet;
    private String tableName;
    private boolean empty;
    private boolean firstTupleRead;

    public DBMSTupleIterator(ResultSet resultSet) {
        this(resultSet, null);
    }

    public DBMSTupleIterator(ResultSet resultSet, String tableName) {
        this.resultSet = resultSet;
        this.tableName = tableName;
        try {
            firstTupleRead = moveResultSet(resultSet);
            if (!firstTupleRead) {
                empty = true;
            }
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public boolean hasNext() {
        try {
            if (empty) {
                return false;
            }
            if (firstTupleRead) {
                return true;
            }
            return !resultSet.isLast();
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public Tuple next() {
        try {
            if (firstTupleRead) {
                firstTupleRead = false;
            } else {
                moveResultSet(resultSet);
            }
            Tuple tuple = DBMSUtility.createTuple(resultSet, tableName);
            return tuple;
        } catch (SQLException ex) {
            throw new DBMSException("Exception in running result set:" + ex);
        }
    }

    public void reset() {
        if (!QueryExecutorConstants.DBMS_DEBUG) {
            throw new UnsupportedOperationException("Unable to reset DBMS result set");
        } else {
            try {
                resultSet.beforeFirst();
            } catch (SQLException ex) {
                throw new DBMSException("Exception in running result set:" + ex);
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void close() {
        QueryManager.closeResultSet(resultSet);
    }

    private boolean moveResultSet(ResultSet resultSet) throws SQLException {
        QueryStatManager.getInstance().addReadTuple();
        return resultSet.next();
    }
}
