package queryexecutor.persistence.file.operators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DAOException;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.operators.sql.AlgebraTreeToSQL;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.DBMSTable;
import queryexecutor.model.database.dbms.DBMSVirtualTable;
import queryexecutor.model.database.operators.dbms.IValueEncoder;
import queryexecutor.model.thread.IBackgroundThread;
import queryexecutor.model.thread.ThreadManager;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.utility.DBMSUtility;
import queryexecutor.utility.QueryExecutorUtility;

public class ExportCSVFileWithCopy {

    private final static Logger logger = LoggerFactory.getLogger(ExportCSVFileWithCopy.class);
    private AlgebraTreeToSQL algebraTreeToSQL = new AlgebraTreeToSQL();

    public void exportDatabase(IDatabase database, IValueEncoder valueEncoder, boolean withHeader, String path, int numberOfThreads) {
//        numberOfThreads = Math.min(numberOfThreads, 5);
        ThreadManager threadManager = new ThreadManager(numberOfThreads);
        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);
            String fileName = path + "/" + table.getName() + ".csv";
            File outputFile = new File(fileName);
            outputFile.getParentFile().mkdirs();
            ExportTableThread execThread = new ExportTableThread(table, valueEncoder, fileName, withHeader);
            threadManager.startThread(execThread);
        }
        threadManager.waitForActiveThread();
    }

    public long exportQuery(IAlgebraOperator query, String queryId, DBMSDB source, DBMSDB target, IValueEncoder valueEncoder, boolean withHeader, String path) {
        String fileName = path + "/" + queryId + ".csv";
        File outputFile = new File(fileName);
        outputFile.getParentFile().mkdirs();
        String sqlQuery = algebraTreeToSQL.treeToSQL(query, source, target, "\t");
        String script = getCopyQueryToScript(sqlQuery, fileName, withHeader, false);
        if (logger.isDebugEnabled()) logger.debug(script);
        return executeCopy(script, fileName, valueEncoder, withHeader, target.getAccessConfiguration());
    }

    public long exportQuery(String sqlQuery, String queryId, DBMSDB target, IValueEncoder valueEncoder, boolean withHeader, String path) {
        String fileName = path + "/" + queryId + ".csv";
        File outputFile = new File(fileName);
        outputFile.getParentFile().mkdirs();
        String script = getCopyQueryToScript(sqlQuery, fileName, withHeader, false);
        if (logger.isDebugEnabled()) logger.debug(script);
        return executeCopy(script, fileName, valueEncoder, withHeader, target.getAccessConfiguration());
    }

    class ExportTableThread implements IBackgroundThread {

        private ITable table;
        private IValueEncoder valueEncoder;
        private String fileName;
        private boolean withHeader;

        public ExportTableThread(ITable table, IValueEncoder valueEncoder, String fileName, boolean withHeader) {
            this.table = table;
            this.valueEncoder = valueEncoder;
            this.fileName = fileName;
            this.withHeader = withHeader;
        }

        public void execute() {
            AccessConfiguration ac = getAccessConfiguration(table);
            String script = getCopyToScript(table, withHeader, ac);
            if (valueEncoder == null) {
                executeCopy(script, fileName, ac);
            } else {
                executeCopy(script, fileName, valueEncoder, withHeader, ac);
            }
        }

    }

    private String getCopyToScript(ITable table, boolean withHeader, AccessConfiguration accessConfiguration) {
        StringBuilder script = new StringBuilder();
        String tableName = getTableName(table);
        script.append("COPY (SELECT ");
        script.append("DISTINCT ");
        for (Attribute attribute : getAttributes(table)) {
            script.append(attribute.getName()).append(", ");
        }
        QueryExecutorUtility.removeChars(", ".length(), script);
        script.append(" FROM ");
        script.append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(tableName).append(" ");
        script.append(") ");
//        if (useFile) {
//            script.append("TO '").append(file).append("' ");
//        } else {
        script.append("TO STDOUT ");
//        }
        script.append(" (");
        script.append("FORMAT CSV, ");
        if (withHeader) {
            script.append("HEADER true");
        } else {
            script.append("HEADER false");
        }
        script.append(", ");
        script.append("QUOTE '\"'");
        script.append(", ");
        script.append("DELIMITER ',')");
//        script.append(";\n");
        if (logger.isDebugEnabled()) logger.debug("--- Import file script:\n" + script.toString());
        return script.toString();
    }

    private String getCopyQueryToScript(String sqlQuery, String file, boolean withHeader, boolean useFile) {
        StringBuilder script = new StringBuilder();
        script.append("COPY (");
        script.append(sqlQuery);
        script.append(") ");
        if (useFile) {
            script.append("TO '").append(file).append("' ");
        } else {
            script.append("TO STDOUT ");
        }
        script.append(" (");
        script.append("FORMAT CSV, ");
        if (withHeader) {
            script.append("HEADER true");
        } else {
            script.append("HEADER false");
        }
        script.append(", ");
        script.append("QUOTE '\"'");
        script.append(", ");
        script.append("DELIMITER ',')");
//        script.append(";\n");
        if (logger.isDebugEnabled()) logger.debug("--- Import file script:\n" + script.toString());
        return script.toString();
    }

    private Iterable<Attribute> getAttributes(ITable table) {
        List<Attribute> result = new ArrayList<Attribute>();
        for (Attribute attribute : table.getAttributes()) {
            if (attribute.getName().equals(QueryExecutorConstants.OID)) {
                continue;
            }
            result.add(attribute);
        }
        return result;
    }

    private long executeCopy(String script, String file, AccessConfiguration ac) {
        Connection con = null;
        PrintWriter writer = null;
        try {
            File parentDir = new File(file).getParentFile();
            parentDir.mkdirs();
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
            con = DriverManager.getConnection(ac.getUri(), ac.getLogin(), ac.getPassword());
            CopyManager copyManager = new CopyManager((BaseConnection) con);
            return copyManager.copyOut(script, new FileOutputStream(file));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        } finally {
            if (writer != null) writer.close();
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private long executeCopy(String script, String file, IValueEncoder valueEncoder, boolean withHeader, AccessConfiguration ac) {
        Connection con = null;
        PrintWriter writer = null;
        try {
            File parentDir = new File(file).getParentFile();
            parentDir.mkdirs();
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
            con = DriverManager.getConnection(ac.getUri(), ac.getLogin(), ac.getPassword());
            CopyManager copyManager = new CopyManager((BaseConnection) con);
            CopyOut copyOut = copyManager.copyOut(script);
            return copyStream(copyOut, valueEncoder, writer, withHeader);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        } finally {
            if (writer != null) writer.close();
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private long copyStream(CopyOut copyOut, IValueEncoder valueEncoder, PrintWriter writer, boolean withHeader) throws SQLException {
        byte[] readFromCopy = copyOut.readFromCopy();
        if (withHeader && readFromCopy != null) {//Header
            writer.write(new String(readFromCopy));
            readFromCopy = copyOut.readFromCopy();
        }
        long count = 0;
        while (readFromCopy != null) {
            count++;
            String line = new String(readFromCopy);
            String[] tokens = line.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String value = tokens[i].trim();
                if (valueEncoder != null) {
                    if (!QueryExecutorUtility.isSkolem(tokens[i])) {
                        value = valueEncoder.decode(value);
                    }
                }
                writer.append(value);
                if (i != tokens.length - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");
            readFromCopy = copyOut.readFromCopy();
        }
        return count;
    }

    private AccessConfiguration getAccessConfiguration(ITable table) {
        if (table instanceof DBMSTable) {
            return ((DBMSTable) table).getAccessConfiguration();
        }
        if (table instanceof DBMSVirtualTable) {
            return ((DBMSVirtualTable) table).getAccessConfiguration();
        }
        throw new IllegalArgumentException("Unable to export non-dbms table using copy command");
    }

    private String getTableName(ITable table) {
        if (table instanceof DBMSTable) {
            return ((DBMSTable) table).getName();
        }
        if (table instanceof DBMSVirtualTable) {
            return ((DBMSVirtualTable) table).getVirtualName();
        }
        throw new IllegalArgumentException("Unable to export non-dbms table using copy command");
    }

}
