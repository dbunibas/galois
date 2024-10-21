package engine.persistence.file.operators;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.exceptions.DAOException;
import engine.model.algebra.operators.ICreateTable;
import engine.model.database.Attribute;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.dbms.InitDBConfiguration;
import engine.model.database.operators.dbms.IValueEncoder;
import engine.persistence.file.CSVFile;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class ImportCSVFileWithCopy {

    private final static Logger logger = LoggerFactory.getLogger(ImportCSVFileWithCopy.class);
    private ICreateTable tableCreator;
    private IValueEncoder valueEncoder;

    public ImportCSVFileWithCopy(ICreateTable tableCreator, IValueEncoder valueEncoder) {
        this.tableCreator = tableCreator;
        this.valueEncoder = valueEncoder;
    }

    public void importCSVFile(String tableName, CSVFile fileToImport, Map<String, List<Attribute>> tablesAdded, DBMSDB database) {
        if (logger.isDebugEnabled()) logger.debug("Importing .csv file using copy...");
        String csvFile = fileToImport.getFileName();
        InitDBConfiguration configuration = database.getInitDBConfiguration();
        if (fileToImport.isRandomizeInput()) {
            throw new DAOException("Randomize input is not available using copy statements");
        }
        Reader in = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Beginning of try block...");
            List<Attribute> attributes;
            CsvMapper mapper = new CsvMapper();
            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(fileToImport.getSeparator());
            if (fileToImport.getQuoteCharacter() != null) {
                schema = schema.withQuoteChar(fileToImport.getQuoteCharacter());
            }
            MappingIterator<String[]> it = mapper.readerFor(String[].class).with(schema).readValues(new File(csvFile));
            if (!it.hasNext()) {
                throw new DAOException("Empty file " + csvFile);
            }
            if (fileToImport.isHasHeader()) {
                String[] headers = it.next();
                attributes = CSVUtility.readCSVAttributes(tableName, headers);
                if (!tablesAdded.containsKey(tableName)) {
                    tablesAdded.put(tableName, attributes);
                    if (configuration.isCreateTablesFromFiles()) {
                        tableCreator.createTable(tableName, attributes, database);
                    }
                }
            } else {
                database.loadTables();
                attributes = EngineUtility.extractAttributesFromDB(tableName, database);
            }
            if (valueEncoder != null || fileToImport.getRecordsToImport() != null) {
                insertCSVTuplesFromStream(tableName, attributes, database, it, fileToImport);
            } else {
                insertCSVTuplesFromFile(tableName, attributes, database, fileToImport);
            }
        } catch (Exception ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
            String message = "Unable to load CSV file " + csvFile;
            if (ex.getMessage() != null && !ex.getMessage().equals("NULL")) {
                message += "\n" + ex.getMessage();
            }
            throw new DAOException(message);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private void insertCSVTuplesFromFile(String tableName, List<Attribute> attributes, DBMSDB database, CSVFile csvFile) {
        if (logger.isDebugEnabled()) logger.debug("Starting to insert csv tuples...");
        Connection con = null;
        try {
            AccessConfiguration ac = database.getAccessConfiguration();
            con = DriverManager.getConnection(ac.getUri(), ac.getLogin(), ac.getPassword());
            CopyManager copyManager = new CopyManager((BaseConnection) con);
            StringBuilder copyScript = new StringBuilder();
            copyScript.append("COPY ").append(DBMSUtility.getSchemaNameAndDot(ac)).append(tableName).append(" (");
            for (Attribute attribute : attributes) {
                copyScript.append(attribute.getName()).append(", ");
            }
            EngineUtility.removeChars(", ".length(), copyScript);
//          script.append(") FROM '").append(csvFile.getFileName()).append("' (");
            copyScript.append(") FROM STDIN (");
            copyScript.append("FORMAT CSV, HEADER ").append(csvFile.isHasHeader() ? "true" : "false");
            copyScript.append(", ");
            if (csvFile.getQuoteCharacter() != null) {
                copyScript.append("QUOTE '").append(csvFile.getQuoteCharacter()).append("'");
                copyScript.append(", ");
            }
            copyScript.append("DELIMITER '").append(csvFile.getSeparator()).append("'");
            copyScript.append(", NULL '").append("null").append("'");
            copyScript.append(" );\n");
            long insertedRows = copyManager.copyIn(copyScript.toString(), new FileInputStream(csvFile.getFileName()));
            if (logger.isDebugEnabled()) logger.debug("Inserted rows: " + insertedRows);
            if (logger.isDebugEnabled()) logger.debug("Rows in file: " + loadFile(csvFile.getFileName(), csvFile.isHasHeader()));
            StringBuilder script = new StringBuilder();
            script.append("ANALYZE ").append(DBMSUtility.getSchemaNameAndDot(ac)).append(tableName).append(";");
            if (logger.isDebugEnabled()) logger.debug("--- Import file script:\n" + script.toString());
            QueryManager.executeScript(script.toString(), ac, true, true, true, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private void insertCSVTuplesFromStream(String tableName, List<Attribute> attributes, DBMSDB database, MappingIterator<String[]> it, CSVFile csvFile) {
        Connection con = null;
        try {
            AccessConfiguration ac = database.getAccessConfiguration();
            con = DriverManager.getConnection(ac.getUri(), ac.getLogin(), ac.getPassword());
            CopyManager copyManager = new CopyManager((BaseConnection) con);
            StringBuilder copyScript = new StringBuilder();
            copyScript.append("COPY ").append(DBMSUtility.getSchemaNameAndDot(ac)).append(tableName).append(" (");
            for (Attribute attribute : attributes) {
                copyScript.append(attribute.getName()).append(", ");
            }
            EngineUtility.removeChars(", ".length(), copyScript);
            copyScript.append(") FROM STDIN (");
            copyScript.append("FORMAT CSV, HEADER ").append(csvFile.isHasHeader() ? "true" : "false");
            copyScript.append(", ");
            if (csvFile.getQuoteCharacter() != null) {
                copyScript.append("QUOTE '").append(csvFile.getQuoteCharacter()).append("'");
                copyScript.append(", ");
            }
            copyScript.append("DELIMITER '").append(csvFile.getSeparator()).append("'");
            copyScript.append(" );\n");
            CopyIn copyIn = copyManager.copyIn(copyScript.toString());
            copyStream(copyIn, it, csvFile.getQuoteCharacter(), csvFile.getSeparator(), csvFile.getRecordsToImport());
            StringBuilder script = new StringBuilder();
            script.append("ANALYZE ").append(DBMSUtility.getSchemaNameAndDot(ac)).append(tableName).append(";");
            if (logger.isDebugEnabled()) logger.debug("--- Import file script:\n" + script.toString());
            QueryManager.executeScript(script.toString(), ac, true, true, true, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private void copyStream(CopyIn copyIn, MappingIterator<String[]> it, Character quoteChar, char separator, Integer maxTuples) throws SQLException {
        int tuples = 0;
        while (it.hasNext() && (maxTuples == null || tuples <= maxTuples)) {
            String[] record = it.next();
            tuples++;
            StringBuilder row = new StringBuilder();
            for (String value : record) {
                String valueToWrite = value;
                if (valueEncoder != null) {
                    valueToWrite = valueEncoder.encode(value);
                }
                if (quoteChar != null) row.append(quoteChar);
                row.append(valueToWrite);
                if (quoteChar != null) row.append(quoteChar);
                row.append(separator);
            }
            EngineUtility.removeChars(1, row);
            row.append("\n");
            byte[] bytes = row.toString().getBytes();
            copyIn.writeToCopy(bytes, 0, bytes.length);
        }
        copyIn.endCopy();
    }

    private long loadFile(String fileName, boolean hasHeader) {
        BufferedReader reader = null;
        long line = 0;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
            while (reader.readLine() != null) {
                line++;
            }
            if (hasHeader) {
                line--;
            }
        } catch (Exception e) {

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {

                }
            }
        }
        return line;
    }
}
