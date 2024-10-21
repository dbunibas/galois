package engine.persistence.relational;

import engine.exceptions.DBMSException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;

public class QueryManager {

    private final static int MAX_LENGTH = 1000;
    private static Logger logger = LoggerFactory.getLogger(QueryManager.class);
//    private static IConnectionFactory dataSourceDB = new SimpleDbConnectionFactory();
    private static IConnectionFactory dataSourceDB = new PooledDbConnectionFactory();

    public static void executeScript(String script, AccessConfiguration accessConfiguration,
            boolean autoCommit, boolean stopOnError, boolean sendFullScript, boolean silentOnError) {
        Connection connection = null;
        try {
            connection = getConnection(accessConfiguration);
            if (logger.isTraceEnabled()) logger.trace("Executing script " + intoSingleLine(script));
            long start = new Date().getTime();
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(autoCommit);
            scriptRunner.setStopOnError(stopOnError);
            scriptRunner.setSendFullScript(sendFullScript);
            if (silentOnError) {
                scriptRunner.setErrorLogWriter(null);
            }
            scriptRunner.runScript(new StringReader(script));
            if (!autoCommit) {
                connection.commit();
            }
            long finish = new Date().getTime();
            if (logger.isDebugEnabled()) logger.debug((finish - start) + " ~ " + intoSingleLine(script));
//            if (!Thread.currentThread().getName().equals("main")) logger.error("Thread: " + Thread.currentThread().getName() + " ~ " + intoSingleLine(script));
            QueryStatManager.getInstance().addQuery(script, (finish - start));
        } catch (Exception daoe) {
            try {
                if (connection != null && !autoCommit) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
            }
            throw new DBMSException("Unable to execute script \n" + script + " on database " + accessConfiguration.getDatabaseName() + ".\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            closeConnection(connection);
        }
    }

    public static int executeInsertOrDelete(String query, AccessConfiguration accessConfiguration) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection(accessConfiguration);
            if (logger.isTraceEnabled()) logger.trace("Executing query " + intoSingleLine(query));
            long start = new Date().getTime();
            statement = connection.createStatement();
            int affectedRows = statement.executeUpdate(query);
            long finish = new Date().getTime();
            if (logger.isTraceEnabled()) logger.trace((finish - start) + " ~ " + intoSingleLine(query));
//            if (!Thread.currentThread().getName().equals("main")) logger.error("Thread: " + Thread.currentThread().getName() + " ~ " + intoSingleLine(query));
            QueryStatManager.getInstance().addQuery(query, (finish - start));
            return affectedRows;
        } catch (Exception daoe) {
            throw new DBMSException("Unable to execute query \n" + query + " on database " + accessConfiguration.getDatabaseName() + ".\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            closeStatement(statement);
            closeConnection(connection);
        }
    }

    public static ResultSet executeQuery(String query, AccessConfiguration accessConfiguration) {
        Connection connection = null;
        try {
            long start = new Date().getTime();
            connection = getConnection(accessConfiguration);
            ResultSet resultSet = executeQuery(query, connection, accessConfiguration);
            long finish = new Date().getTime();
            if (logger.isTraceEnabled()) logger.trace((finish - start) + " ~ " + intoSingleLine(query));
            QueryStatManager.getInstance().addQuery(query, (finish - start));
//            if (!Thread.currentThread().getName().equals("main")) logger.error("Thread: " + Thread.currentThread().getName() + " ~ " + intoSingleLine(query));
            return resultSet;
        } catch (Exception daoe) {
            throw new DBMSException("Unable to execute query \n" + query + " on database " + accessConfiguration.getDatabaseName() + ".\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        }
    }

    public static ResultSet executeQuery(String query, Connection connection, AccessConfiguration accessConfiguration) {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection.setAutoCommit(false);
            if (logger.isTraceEnabled()) logger.trace("Executing query " + intoSingleLine(query));
            long start = new Date().getTime();
            if (EngineConstants.DBMS_DEBUG) {
                statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } else {
                statement = connection.createStatement();
            }
            resultSet = statement.executeQuery(query);
            long finish = new Date().getTime();
            if (logger.isDebugEnabled()) logger.debug((finish - start) + " ~ " + intoSingleLine(query));
//            if (!Thread.currentThread().getName().equals("main")) logger.error("Thread: " + Thread.currentThread().getName() + " ~ " + intoSingleLine(query));
            QueryStatManager.getInstance().addQuery(query, (finish - start));
        } catch (Exception daoe) {
            throw new DBMSException("Unable to execute query \n" + query + " on database " + accessConfiguration.getDatabaseName() + ".\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        }
        return resultSet;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    public static Connection getConnection(AccessConfiguration accessConfiguration) {
//        if (!Thread.currentThread().getName().equals("main")) logger.error("Thread: " + Thread.currentThread().getName() + " - getting connection" + accessConfiguration.getDatabaseName() + "." + accessConfiguration.getSchemaName());
        return dataSourceDB.getConnection(accessConfiguration);
    }

    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }
        try {
            Statement statement = resultSet.getStatement();
            resultSet.close();
            closeStatement(statement);
        } catch (Exception daoe) {
            logger.warn("Unable to close result set. " + daoe);
        }
    }

    private static void closeStatement(Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            Connection connection = statement.getConnection();
            statement.close();
            closeConnection(connection);
        } catch (Exception daoe) {
            logger.warn("Unable to close statement. " + daoe);
        }
    }

    public static void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException sqle) {
            logger.warn("Unable to close connection. " + sqle.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    private static ScriptRunner getScriptRunner(Connection connection) {
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        scriptRunner.setLogWriter(null);
//        scriptRunner.setErrorLogWriter(null);
        return scriptRunner;
    }

    private static String intoSingleLine(String query) {
        if (query.startsWith("INSERT INTO") && query.length() > MAX_LENGTH) {
            query = query.substring(0, MAX_LENGTH) + "...";
        }
        return query.replaceAll("\n", " ");
    }

    public static void setDataSourceDB(IConnectionFactory dataSourceDB) {
        QueryManager.dataSourceDB = dataSourceDB;
    }

    public static void close() {
        dataSourceDB.close();
    }
}
