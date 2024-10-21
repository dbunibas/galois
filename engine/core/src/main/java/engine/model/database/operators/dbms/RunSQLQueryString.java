package engine.model.database.operators.dbms;

import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.IDatabase;
import engine.model.database.dbms.DBMSTupleIterator;
import engine.model.database.dbms.SQLQueryString;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;

public class RunSQLQueryString {

    private final static Logger logger = LoggerFactory.getLogger(RunSQLQueryString.class);

    public ITupleIterator runQuery(SQLQueryString sqlQuery, IDatabase database) {
        AccessConfiguration accessConfiguration = DBMSUtility.getAccessConfiguration(database);
        if (logger.isDebugEnabled()) logger.debug("Executing sql \n" + sqlQuery);
        ResultSet resultSet = QueryManager.executeQuery(sqlQuery.getQuery(), accessConfiguration);
        return new DBMSTupleIterator(resultSet);
    }

}
