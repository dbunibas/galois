package queryexecutor.model.database.operators.dbms;

import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.dbms.DBMSTupleIterator;
import queryexecutor.model.database.dbms.SQLQueryString;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.utility.DBMSUtility;

public class RunSQLQueryString {

    private final static Logger logger = LoggerFactory.getLogger(RunSQLQueryString.class);

    public ITupleIterator runQuery(SQLQueryString sqlQuery, IDatabase database) {
        AccessConfiguration accessConfiguration = DBMSUtility.getAccessConfiguration(database);
        if (logger.isDebugEnabled()) logger.debug("Executing sql \n" + sqlQuery);
        ResultSet resultSet = QueryManager.executeQuery(sqlQuery.getQuery(), accessConfiguration);
        return new DBMSTupleIterator(resultSet);
    }

}
