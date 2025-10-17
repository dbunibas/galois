package queryexecutor.utility;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DAOException;
import queryexecutor.exceptions.DBMSException;
import queryexecutor.model.database.*;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.DBMSVirtualDB;
import queryexecutor.model.database.mainmemory.datasource.IntegerOIDGenerator;
import queryexecutor.model.database.operators.lazyloading.DBMSTupleLoader;
import queryexecutor.persistence.Types;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.persistence.relational.SimpleDbConnectionFactory;

import java.io.StringReader;
import java.sql.*;
import java.util.Date;
import java.util.*;

public class DBMSUtility {

    private static Logger logger = LoggerFactory.getLogger(DBMSUtility.class);

    public static final String TEMP_DB_NAME_POSTGRES = "template1";
    public static final String TEMP_DB_NAME_MYSQL = "mysql";
    private static SimpleDbConnectionFactory simpleDataSourceDB = new SimpleDbConnectionFactory();

    public static List<String> loadTableNames(AccessConfiguration accessConfiguration) {
        List<String> tableNames = new ArrayList<String>();
        String schemaName = accessConfiguration.getSchemaAndSuffix();
        ResultSet tableResultSet = null;
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Loading table names: " + accessConfiguration);
            connection = QueryManager.getConnection(accessConfiguration);
            String catalog = connection.getCatalog();
            if (catalog == null) {
                catalog = accessConfiguration.getUri();
                if (logger.isDebugEnabled()) logger.debug("Catalog is null. Catalog name will be: " + catalog);
            }
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            tableResultSet = databaseMetaData.getTables(catalog, schemaName, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        } catch (DAOException daoe) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } catch (SQLException sqle) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + sqle.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(tableResultSet);
            QueryManager.closeConnection(connection);
        }
        return tableNames;
    }

    public static List<Key> loadKeys(AccessConfiguration accessConfiguration) {
        List<Key> result = new ArrayList<Key>();
        String schemaName = accessConfiguration.getSchemaAndSuffix();
        Connection connection = null;
        ResultSet tableResultSet = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Loading keys: " + accessConfiguration);
            connection = QueryManager.getConnection(accessConfiguration);
            String catalog = connection.getCatalog();
            if (catalog == null) {
                catalog = accessConfiguration.getUri();
                if (logger.isDebugEnabled()) logger.debug("Catalog is null. Catalog name will be: " + catalog);
            }
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            tableResultSet = databaseMetaData.getTables(catalog, schemaName, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                List<AttributeRef> attributes = new ArrayList<AttributeRef>();
                String tableName = tableResultSet.getString("TABLE_NAME");
                if (logger.isDebugEnabled()) logger.debug("Searching unique. ANALYZING TABLE  = " + tableName);
                boolean unique = true;
                ResultSet resultSet = databaseMetaData.getIndexInfo(catalog, null, tableName, unique, true);
                while (resultSet.next()) {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    if (logger.isDebugEnabled()) logger.debug("Analyzing unique: " + columnName);
                    if (logger.isDebugEnabled()) logger.debug("Found a unique: " + columnName);
                    AttributeRef attributeRef = new AttributeRef(tableName, columnName);
                    if (!attributes.contains(attributeRef)) {
                        Key key = new Key(attributeRef);
                        result.add(key);
                        attributes.add(attributeRef);
                    }
                }
            }
        } catch (DAOException daoe) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } catch (SQLException sqle) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + sqle.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(tableResultSet);
            QueryManager.closeConnection(connection);
        }
        return result;
    }

    public static List<Key> loadPrimaryKeys(AccessConfiguration accessConfiguration) {
        List<Key> result = new ArrayList<>();
        String schemaName = accessConfiguration.getSchemaAndSuffix();
        Connection connection = null;
        ResultSet tableResultSet = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Loading primary keys: " + accessConfiguration);
            connection = QueryManager.getConnection(accessConfiguration);
            String catalog = connection.getCatalog();
            if (catalog == null) {
                catalog = accessConfiguration.getUri();
                if (logger.isDebugEnabled()) logger.debug("Catalog is null. Catalog name will be: " + catalog);
            }
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            tableResultSet = databaseMetaData.getTables(catalog, schemaName, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                List<AttributeRef> attributes = new ArrayList<AttributeRef>();
                String tableName = tableResultSet.getString("TABLE_NAME");
                if (logger.isDebugEnabled()) logger.debug("Searching unique. ANALYZING TABLE  = " + tableName);
                ResultSet resultSet = databaseMetaData.getPrimaryKeys(catalog, null, tableName);
                while (resultSet.next()) {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    if (logger.isDebugEnabled()) logger.debug("Analyzing: " + columnName);
                    if (logger.isDebugEnabled()) logger.debug("Found a primary key: " + columnName);
                    AttributeRef attributeRef = new AttributeRef(tableName, columnName);
                    if (!attributes.contains(attributeRef)) {
                        attributes.add(attributeRef);
                    }
                }
                if (!attributes.isEmpty()) result.add(new Key(attributes, true));
            }
        } catch (DAOException | SQLException daoe) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(tableResultSet);
            QueryManager.closeConnection(connection);
        }
        return result;
    }

    public static List<ForeignKey> loadForeignKeys(AccessConfiguration accessConfiguration) {
        Map<String, ForeignKey> foreignKeyMap = new HashMap<String, ForeignKey>();
        String schemaName = accessConfiguration.getSchemaAndSuffix();
        Connection connection = null;
        ResultSet tableResultSet = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Loading foreign keys: " + accessConfiguration);
            connection = QueryManager.getConnection(accessConfiguration);
            String catalog = connection.getCatalog();
            if (catalog == null) {
                catalog = accessConfiguration.getUri();
                if (logger.isDebugEnabled()) logger.debug("Catalog is null. Catalog name will be: " + catalog);
            }
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            tableResultSet = databaseMetaData.getTables(catalog, schemaName, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString("TABLE_NAME");
                if (logger.isDebugEnabled()) logger.debug("Searching foreign keys. ANALYZING TABLE  = " + tableName);
                ResultSet resultSet = databaseMetaData.getImportedKeys(catalog, null, tableName);
                while (resultSet.next()) {
                    String fkeyName = resultSet.getString("FK_NAME");
                    String pkTableName = resultSet.getString("PKTABLE_NAME");
                    String pkColumnName = resultSet.getString("PKCOLUMN_NAME");
                    String keyPrimaryKey = pkTableName + "." + pkColumnName;
                    String fkTableName = resultSet.getString("FKTABLE_NAME");
                    String fkColumnName = resultSet.getString("FKCOLUMN_NAME");
                    String keyForeignKey = fkTableName + "." + fkColumnName;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Analyzing Primary Key: " + keyPrimaryKey + " Found a Foreign Key: " + fkColumnName + " in table " + fkTableName);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Analyzing foreign key: " + keyForeignKey + " references " + keyPrimaryKey);
                    }
                    addFKToMap(foreignKeyMap, fkeyName, new AttributeRef(pkTableName, pkColumnName), new AttributeRef(fkTableName, fkColumnName));
                }
            }
        } catch (DAOException daoe) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } catch (SQLException sqle) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + sqle.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(tableResultSet);
            QueryManager.closeConnection(connection);
        }
        return new ArrayList<ForeignKey>(foreignKeyMap.values());
    }

    private static void addFKToMap(Map<String, ForeignKey> foreignKeyMap, String fkeyName, AttributeRef keyAttribute, AttributeRef refAttribute) {
        ForeignKey fk = foreignKeyMap.get(fkeyName);
        if (fk == null) {
            fk = new ForeignKey(keyAttribute, refAttribute);
            foreignKeyMap.put(fkeyName, fk);
            return;
        }
        fk.getKeyAttributes().add(keyAttribute);
        fk.getRefAttributes().add(refAttribute);
    }

    public static boolean isDBExists(AccessConfiguration accessConfiguration) {
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Checking if db exists: " + accessConfiguration);
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            return true;
        } catch (DAOException daoe) {
            if (logger.isDebugEnabled()) logger.debug("Error checking if db exists: " + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeConnection(connection);
        }
        return false;
    }

    public static boolean isSchemaExists(AccessConfiguration accessConfiguration) {
        Connection connection = null;
        ResultSet schemaResultSet = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Checking if schema exists: " + accessConfiguration);
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            schemaResultSet = databaseMetaData.getSchemas();
            while (schemaResultSet.next()) {
                String schemaName = schemaResultSet.getString("TABLE_SCHEM");
                if (schemaName.equals(accessConfiguration.getSchemaAndSuffix())) {
                    return true;
                }
            }
        } catch (Exception daoe) {
            if (logger.isDebugEnabled()) logger.debug("Error checking if schema exists: " + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(schemaResultSet);
            QueryManager.closeConnection(connection);
        }
        return false;
    }

    public static boolean isSchemaEmpty(AccessConfiguration accessConfiguration) {
        if (supportsSchema(accessConfiguration) && !isSchemaExists(accessConfiguration)) {
            return true;
        }
        List<String> tableNames = loadTableNames(accessConfiguration);
        return tableNames.isEmpty();
    }

    public static void createDB(AccessConfiguration accessConfiguration) {
        AccessConfiguration tempAccessConfiguration = getTempAccessConfiguration(accessConfiguration);
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Creating db: " + accessConfiguration);
            connection = simpleDataSourceDB.getConnection(tempAccessConfiguration);
            String createQuery = "create database " + accessConfiguration.getDatabaseName() + ";";
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(new StringReader(createQuery));
        } catch (Exception daoe) {
            throw new DBMSException("Unable to create new database " + accessConfiguration.getDatabaseName() + ".\n" + tempAccessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    public static void deleteDB(AccessConfiguration accessConfiguration) {
        String script = "DROP DATABASE " + accessConfiguration.getDatabaseName() + ";\n";
        AccessConfiguration tempAccessConfiguration = getTempAccessConfiguration(accessConfiguration);
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Deleting db: " + accessConfiguration);
            connection = simpleDataSourceDB.getConnection(tempAccessConfiguration);
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.setErrorLogWriter(null);
            scriptRunner.runScript(new StringReader(script));
        } catch (Exception daoe) {
            throw new DBMSException("Unable to drop database " + accessConfiguration.getDatabaseName() + ".\n" + tempAccessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    private static ScriptRunner getScriptRunner(Connection connection) {
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        scriptRunner.setLogWriter(null);
//        scriptRunner.setErrorLogWriter(null);
        return scriptRunner;
    }

    public static ResultSet getTableResultSetSortByOID(String tableName, AccessConfiguration accessConfiguration) {
        String query = "SELECT " + QueryExecutorConstants.OID + ",* FROM " + getSchemaNameAndDot(accessConfiguration) + "\"" + tableName + "\"" + " ORDER BY " + QueryExecutorConstants.OID;
        return QueryManager.executeQuery(query, accessConfiguration);
    }

    public static ResultSet getTableResultSetUnsorted(String tableName, AccessConfiguration accessConfiguration) {
        String query = "SELECT " + QueryExecutorConstants.OID + ",* FROM " + getSchemaNameAndDot(accessConfiguration) + "\"" + tableName + "\"";
        return QueryManager.executeQuery(query, accessConfiguration);
    }

    public static String createTablePaginationQuery(String tableName, AccessConfiguration accessConfiguration, int offset, int limit) {
        return "SELECT " + QueryExecutorConstants.OID + ",* FROM " + getSchemaNameAndDot(accessConfiguration) + "\"" + tableName + "\"" + " ORDER BY " + QueryExecutorConstants.OID + " LIMIT " + limit + " OFFSET " + offset;
    }

    public static ResultSet getTableOidsResultSet(String tableName, AccessConfiguration accessConfiguration) {
        String query = "SELECT " + QueryExecutorConstants.OID + " FROM " + getSchemaNameAndDot(accessConfiguration) + "\"" + tableName + "\"";
        return QueryManager.executeQuery(query, accessConfiguration);
    }

    public static ResultSet getTupleResultSet(String tableName, TupleOID oid, AccessConfiguration accessConfiguration, Connection c) {
        String query = "SELECT " + QueryExecutorConstants.OID + ",* FROM " + getSchemaNameAndDot(accessConfiguration) + "\"" + tableName + "\"" + " WHERE " + QueryExecutorConstants.OID + "=" + oid.toString();
        return QueryManager.executeQuery(query, c, accessConfiguration);
    }

    public static ResultSet getTableResultSetForSchema(String tableName, AccessConfiguration accessConfiguration) {
        String query = "SELECT " + QueryExecutorConstants.OID + ", " + getTableName(tableName, accessConfiguration) + ".* FROM " + getSchemaNameAndDot(accessConfiguration) + tableName + " LIMIT 0";
        return QueryManager.executeQuery(query, accessConfiguration);
    }

    public static List<Attribute> loadAttributesFromTable(String tableName, AccessConfiguration accessConfiguration) {
        List<Attribute> result = new ArrayList<Attribute>();
        Connection connection = null;
        ResultSet tableResultSet = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Loading attributes: " + accessConfiguration);
            connection = QueryManager.getConnection(accessConfiguration);
            Statement stmt = connection.createStatement();
            String query = "SELECT " + QueryExecutorConstants.OID + ",* FROM " + getSchemaNameAndDot(accessConfiguration) + tableName + " LIMIT 1";
            if (logger.isDebugEnabled()) logger.debug(query);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String columnName = rsmd.getColumnName(i);
                String columnType = rsmd.getColumnTypeName(i);
                int isNullable = rsmd.isNullable(i);
                if (logger.isDebugEnabled()) logger.debug("Column name: " + columnName);
                if (logger.isDebugEnabled()) logger.debug("Column type: " + columnType);
                if (logger.isDebugEnabled()) logger.debug("Nullable: " + isNullable);
                Attribute attribute = new Attribute(tableName, columnName, DBMSUtility.convertDBTypeToDataSourceType(columnType));
                attribute.setNullable(isNullable == 0);
                if (logger.isDebugEnabled()) logger.debug("Attribute: " + attribute);
                QueryExecutorUtility.addIfNotContained(result, attribute);
            }
        } catch (DAOException daoe) {
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DBMSException("Error connecting to database.\n" + accessConfiguration + "\n" + sqle.getLocalizedMessage());
        } finally {
            QueryManager.closeResultSet(tableResultSet);
            QueryManager.closeConnection(connection);
        }
        return result;
    }

    private static String getTableName(String tableName, AccessConfiguration accessConfiguration) {
        if (isMySQL(accessConfiguration.getDriver())) {
            return tableName;
        }
        return "\"" + tableName + "\"";
    }

    public static String getSchemaNameAndDot(AccessConfiguration accessConfiguration) {
        if (!supportsSchema(accessConfiguration)) {
            return "";
        }
        String schemaName = accessConfiguration.getSchemaAndSuffix();
        if (schemaName == null || schemaName.isEmpty()) {
            return "";
        }
        return accessConfiguration.getSchemaAndSuffix() + ".";
    }

    public static Tuple createTupleFromQuery(ResultSet resultSet) {
        return createTuple(resultSet, null);
    }

    public static DBMSTupleLoader createTupleLoader(ResultSet resultSet, String tableName, String virtualTableName, AccessConfiguration configuration) {
        try {
            ResultSetMetaData metadata = resultSet.getMetaData();
            Object oidValue = findOIDColumn(metadata, resultSet);
            if (metadata.getColumnCount() >= 1 && metadata.getColumnName(1).equals(QueryExecutorConstants.OID)) {
                oidValue = resultSet.getObject(1);
            }
            TupleOID tupleOID = new TupleOID(oidValue);
            DBMSTupleLoader tuple = new DBMSTupleLoader(tableName, virtualTableName, tupleOID, configuration);
            return tuple;
        } catch (Exception daoe) {
            daoe.printStackTrace();
            throw new DBMSException("Unable to read tuple.\n" + daoe.getLocalizedMessage());
        }
    }

    public static Tuple createTuple(ResultSet resultSet, String tableName) {
        try {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int startColumn = 1;
            Object oidValue = findOIDColumn(metadata, resultSet);
            if (metadata.getColumnCount() >= 1 && metadata.getColumnName(1).equals(QueryExecutorConstants.OID)) {
                oidValue = resultSet.getObject(1);
                startColumn = 2;
            }
            if (metadata.getColumnCount() >= 2 && metadata.getColumnName(2).equals(QueryExecutorConstants.OID)) {
                startColumn = 3;
            }
            TupleOID tupleOID = new TupleOID(oidValue);
            Tuple tuple = new Tuple(tupleOID);
            Cell oidCell = new Cell(tupleOID, new AttributeRef(tableName, QueryExecutorConstants.OID), new ConstantValue(tupleOID));
            tuple.addCell(oidCell);
            int columns = metadata.getColumnCount();
            for (int col = startColumn; col <= columns; col++) {
                String attributeName = metadata.getColumnLabel(col);
                Object attributeValue = resultSet.getObject(col);
                IValue value = convertDBMSValue(attributeValue);
                AttributeRef attributeRef;
                if (tableName != null) {
                    attributeRef = new AttributeRef(tableName, attributeName);
                } else {
                    attributeRef = extractAttributeRef(attributeName);
                }
                Cell cell = new Cell(tupleOID, attributeRef, value);
                tuple.addCell(cell);
            }
            return tuple;
        } catch (Exception daoe) {
            daoe.printStackTrace();
            throw new DBMSException("Unable to read tuple.\n" + daoe.getLocalizedMessage());
        }
    }

    private static AttributeRef extractAttributeRef(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Unable to extract attribute from empty string");
        }
        boolean source = false;
        if (value.startsWith("source_")) {
            source = true;
            value = value.substring("source_".length());
        }
        int lastIndex = value.lastIndexOf(QueryExecutorConstants.DELTA_TABLE_SEPARATOR);
        if (lastIndex == -1) {
            return new AttributeRef("", value);
//            throw new IllegalArgumentException("Unable to extract attribute from string " + value);
        }
        String tableAliasSQL = value.substring(0, lastIndex);
        String attributeSQL = value.substring(lastIndex + QueryExecutorConstants.DELTA_TABLE_SEPARATOR.length());
        TableAlias tableAlias;
        if (tableAliasSQL.contains(QueryExecutorConstants.DELTA_TABLE_SEPARATOR)) {
            int firstIndex = tableAliasSQL.indexOf(QueryExecutorConstants.DELTA_TABLE_SEPARATOR);
            String tableName = tableAliasSQL.substring(0, firstIndex);
            String alias = tableAliasSQL.substring(firstIndex + QueryExecutorConstants.DELTA_TABLE_SEPARATOR.length());
            tableAlias = new TableAlias(tableName, alias);
        } else {
            tableAlias = new TableAlias(tableAliasSQL);
        }
        tableAlias.setSource(source);
        AttributeRef attributeRef = new AttributeRef(tableAlias, attributeSQL);
        return attributeRef;
    }

    private static Object findOIDColumn(ResultSetMetaData metadata, ResultSet resultSet) throws SQLException {
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            if (metadata.getColumnName(i).equalsIgnoreCase(QueryExecutorConstants.OID)) {
                return resultSet.getObject(i);
            }
        }
        return IntegerOIDGenerator.getNextOID();
    }

    public static List<Attribute> getTableAttributes(ResultSet resultSet, String tableName) throws SQLException {
        List<Attribute> result = new ArrayList<Attribute>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columns = metadata.getColumnCount();
        for (int col = 1; col <= columns; col++) {
            String attributeName = metadata.getColumnName(col);
            String attributeType = metadata.getColumnTypeName(col);
            if (logger.isDebugEnabled()) {
                logger.debug("Table: " + tableName + " - Attribute: " + attributeName + " - Type: " + attributeType);
            }
            Attribute attribute = new Attribute(tableName, attributeName, DBMSUtility.convertDBTypeToDataSourceType(attributeType));
            result.add(attribute);
        }
        return result;
    }

    public static AccessConfiguration getTempAccessConfiguration(AccessConfiguration accessConfiguration) {
        AccessConfiguration tmpAccess = new AccessConfiguration();
        tmpAccess.setDriver(accessConfiguration.getDriver());
        tmpAccess.setUri(getTempDBName(accessConfiguration, getDefaultDBName(accessConfiguration.getDriver())));
        tmpAccess.setLogin(accessConfiguration.getLogin());
        tmpAccess.setPassword(accessConfiguration.getPassword());
        return tmpAccess;
    }

    private static String getDefaultDBName(String driver) {
        if (driver.toLowerCase().contains("postgres")) {
            return TEMP_DB_NAME_POSTGRES;
        }
        if (driver.toLowerCase().contains("mysql")) {
            return TEMP_DB_NAME_MYSQL;
        }
        throw new IllegalArgumentException("Unable to return default database name for dbms " + driver);
    }

    public static boolean supportsSchema(AccessConfiguration accessConfiguration) {
        if (accessConfiguration.getDriver().toLowerCase().contains("postgres")) {
            return true;
        }
        return false;
    }

    private static String getTempDBName(AccessConfiguration accessConfiguration, String tempDBName) {
        String uri = accessConfiguration.getUri();
        if (uri.lastIndexOf("/") != -1) {
            return uri.substring(0, uri.lastIndexOf("/") + 1) + tempDBName;
        }
        return uri.substring(0, uri.lastIndexOf(":") + 1) + tempDBName;
    }

    public static String convertDBTypeToDataSourceType(String columnType) {
        if (columnType.equalsIgnoreCase("varchar") || columnType.equalsIgnoreCase("char")
                || columnType.equalsIgnoreCase("text") || columnType.equalsIgnoreCase("bpchar")
                || columnType.equalsIgnoreCase("bit") || columnType.equalsIgnoreCase("mediumtext")
                || columnType.equalsIgnoreCase("longtext")) {
            return Types.STRING;
        }
        if (columnType.equalsIgnoreCase("enum")) {
            return Types.STRING;
        }
        if (columnType.equalsIgnoreCase("date")) {
            return Types.DATE;
        }
        if (columnType.equalsIgnoreCase("datetime") || columnType.equalsIgnoreCase("timestamp") || columnType.equalsIgnoreCase("time")) {
            return Types.DATETIME;
        }
        if (columnType.equalsIgnoreCase("bigserial") || columnType.equalsIgnoreCase("serial8") || columnType.toLowerCase().equals("int8") || columnType.toLowerCase().startsWith("bigint")) {
            return Types.LONG;
        }
        if (columnType.toLowerCase().equals("float8") || columnType.toLowerCase().startsWith("double precision")) {
            return Types.DOUBLE_PRECISION;
        }
        if (columnType.toLowerCase().startsWith("serial") || columnType.toLowerCase().startsWith("int") || columnType.toLowerCase().startsWith("tinyint") || columnType.toLowerCase().startsWith("smallint")) {
            return Types.INTEGER;
        }
        if (columnType.toLowerCase().startsWith("float") || columnType.toLowerCase().startsWith("real")) {
            return Types.REAL;
        }
        if (columnType.equalsIgnoreCase("bool")) {
            return Types.BOOLEAN;
        }
        return Types.STRING;
    }

    public static String convertDataSourceTypeToDBType(String columnType) {
        if (columnType.equalsIgnoreCase(Types.DATE)) {
            return "date";
        }
        if (columnType.equalsIgnoreCase(Types.DATETIME)) {
            return "datetime";
        }
        if (columnType.equalsIgnoreCase(Types.INTEGER)) {
            return "bigint";
        }
        if (columnType.equalsIgnoreCase(Types.REAL)) {
            return "float";
        }
        if (columnType.equalsIgnoreCase(Types.BOOLEAN)) {
            return "bool";
        }
        if (columnType.equalsIgnoreCase(Types.LONG)) {
            return "int8";
        }
        if (columnType.equalsIgnoreCase(Types.DOUBLE_PRECISION)) {
            return "float8";
        }
        return "text";
    }

    public static IValue convertDBMSValue(Object attributeValue) {
        IValue value;
        if (attributeValue == null || attributeValue.toString().equalsIgnoreCase(QueryExecutorConstants.NULL)) {
            value = new NullValue(QueryExecutorConstants.NULL_VALUE);
        } else if (QueryExecutorUtility.isSkolem(attributeValue)) {
            value = new NullValue(attributeValue);
        } else if (QueryExecutorUtility.isVariable(attributeValue)) {
            value = new LLUNValue(attributeValue);
        } else {
            value = new ConstantValue(attributeValue);
        }
        return value;
    }

    public static String tableAliasToSQL(TableAlias tableAlias) {
        StringBuilder result = new StringBuilder();
        result.append(tableAlias.isSource() ? "source_" : "");
        result.append(tableAlias.getTableName());
        result.append(tableAlias.getAlias().equals("") ? "" : QueryExecutorConstants.DELTA_TABLE_SEPARATOR + tableAlias.getAlias());
        return result.toString();
    }

    public static String attributeRefToSQL(AttributeRef attribureRef) {
        String tableAliasScript = DBMSUtility.tableAliasToSQL(attribureRef.getTableAlias());
        if (!tableAliasScript.isEmpty()) {
            tableAliasScript += QueryExecutorConstants.DELTA_TABLE_SEPARATOR;
        }
        String result = tableAliasScript + attribureRef.getName();
        return result;
    }

    public static String attributeRefToSQLDot(AttributeRef attributeRef) {
        StringBuilder sb = new StringBuilder();
        sb.append(DBMSUtility.tableAliasToSQL(attributeRef.getTableAlias())).append(".").append(attributeRef.getName());
        return sb.toString();
    }

    public static String attributeRefToAliasSQL(AttributeRef attributeRef) {
        StringBuilder sb = new StringBuilder();
        sb.append(DBMSUtility.tableAliasToSQL(attributeRef.getTableAlias())).append(QueryExecutorConstants.DELTA_TABLE_SEPARATOR).append(attributeRef.getName());
        return sb.toString();
    }

    public static String cleanRelationName(String name) {
        String clean = name;
        clean = clean.replaceAll("-", QueryExecutorConstants.DELTA_TABLE_SEPARATOR);
        return clean;
    }

    public static void createSchema(AccessConfiguration accessConfiguration) {
        StringBuilder result = new StringBuilder();
        result.append("DROP SCHEMA IF EXISTS ").append(accessConfiguration.getSchemaAndSuffix()).append(" CASCADE;\n");
        result.append("CREATE SCHEMA ").append(accessConfiguration.getSchemaAndSuffix()).append(";\n\n");
        QueryManager.executeScript(result.toString(), accessConfiguration, true, true, false, true);
    }

    public static void removeSchema(String schema, AccessConfiguration accessConfiguration) {
        String script = "DROP SCHEMA IF EXISTS " + schema + " CASCADE;";
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Dropping schema: " + schema);
            }
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(new StringReader(script));
        } catch (Exception daoe) {
            throw new DBMSException("Unable to drop schema " + accessConfiguration.getDatabaseName() + ".\n" + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    public static void createFunctionsForNumericalSkolem(AccessConfiguration accessConfiguration) {
        StringBuilder script = new StringBuilder();
        script.append("drop function if exists bigint_skolem(text); create function bigint_skolem(text) returns bigint as $$\n"
                + " select ('" + QueryExecutorConstants.BIGINT_SKOLEM_PREFIX + "' || @('x'||substr(md5($1),1,13))::bit(52)::bigint)::bigint;\n"
                + "$$ language sql;\n"
                + "\n"
                + "drop function if exists double_skolem(text); create or replace function double_skolem(text) returns double precision as $$\n"
                + " select ('" + QueryExecutorConstants.BIGINT_SKOLEM_PREFIX + "' || @('x'||substr(md5($1),1,13))::bit(52)::bigint)::double precision;\n"
                + "$$ language sql;\n"
                + "\n");
        Connection connection = null;
        try {
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.setSendFullScript(true);
            scriptRunner.runScript(new StringReader(script.toString()));
        } catch (Exception daoe) {
            daoe.printStackTrace();
            throw new DBMSException("Unable to execute\n" + script + "\n on " + accessConfiguration + "\n" + daoe.getLocalizedMessage());
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    public static boolean isMySQL(String driver) {
        return driver.toLowerCase().contains("mysql");
    }

    public static AccessConfiguration getAccessConfiguration(IDatabase target) throws IllegalArgumentException {
        AccessConfiguration accessConfiguration;
        if (target instanceof DBMSDB) {
            accessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        } else if (target instanceof DBMSVirtualDB) {
            accessConfiguration = ((DBMSVirtualDB) target).getAccessConfiguration();
        } else {
            throw new IllegalArgumentException("Unable to execute SQL on main memory db. " + target);
        }
        return accessConfiguration;
    }

    //    public static void cleanWorkTargetSchemas(AccessConfiguration accessConfiguration) {
//        if (accessConfiguration == null) {
//            return;
//        }
//        try {
//            PrintUtility.printInformation("Removing schema " + accessConfiguration.getSchemaName() + " and "
//                    + QueryExecutorConstants.WORK_SCHEMA + ", if exist...");
//            DBMSUtility.removeSchema(accessConfiguration.getSchemaName(), accessConfiguration);
//            DBMSUtility.removeSchema(QueryExecutorConstants.WORK_SCHEMA, accessConfiguration);
//            PrintUtility.printSuccess("Schemas removed!");
//        } catch (DBMSException ex) {
//            String message = ex.getMessage();
//            if (!message.contains("does not exist")) {
//                PrintUtility.printError("Unable to drop schema.\n" + ex.getLocalizedMessage());
//            }
//        }
//    }
    public static void cleanWorkTargetSchemas(AccessConfiguration accessConfiguration) {
        if (accessConfiguration == null) {
            return;
        }
        StringBuilder script = new StringBuilder();
        script.append("DROP SCHEMA IF EXISTS ").append(accessConfiguration.getSchemaName()).append(" CASCADE;");
        script.append("DROP SCHEMA IF EXISTS ").append(QueryExecutorConstants.WORK_SCHEMA).append(" CASCADE;");
        script.append("VACUUM FULL;");
        Connection connection = null;
        try {
            PrintUtility.printInformation("Removing schema " + accessConfiguration.getSchemaName() + " and "
                    + QueryExecutorConstants.WORK_SCHEMA + ", if exist...");
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(new StringReader(script.toString()));
            PrintUtility.printSuccess("Schemas removed!");
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (!message.contains("does not exist")) {
                PrintUtility.printError("Unable to drop schema.\n" + ex.getLocalizedMessage());
                throw new DBMSException("Unable to drop schema " + accessConfiguration.getDatabaseName() + ".\n" + ex.getLocalizedMessage());
            }
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    public static void renameExistingWorkTargetSchemas(AccessConfiguration accessConfiguration) {
        if (accessConfiguration == null) {
            return;
        }
        StringBuilder script = new StringBuilder();
        script.append(getRenamingScript(accessConfiguration.getSchemaName()));
        script.append(getRenamingScript(QueryExecutorConstants.WORK_SCHEMA));
//        script.append("VACUUM FULL;");
        Connection connection = null;
        try {
            PrintUtility.printInformation("Removing schema " + accessConfiguration.getSchemaName() + " and "
                    + QueryExecutorConstants.WORK_SCHEMA + ", if exist...");
            connection = simpleDataSourceDB.getConnection(accessConfiguration);
            ScriptRunner scriptRunner = getScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(false);
            scriptRunner.setErrorLogWriter(null);
            scriptRunner.runScript(new StringReader(script.toString()));
            PrintUtility.printSuccess("Schemas removed!");
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (!message.contains("does not exist")) {
                PrintUtility.printError("Unable to drop schema.\n" + ex.getLocalizedMessage());
                throw new DBMSException("Unable to drop schema " + accessConfiguration.getDatabaseName() + ".\n" + ex.getLocalizedMessage());
            }
        } finally {
            QueryManager.closeConnection(connection);
        }
    }

    private static String getRenamingScript(String schemaName) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER SCHEMA ").append(schemaName);
        sb.append(" RENAME TO ").append(schemaName).append("_").append(new Date().getTime()).append(";\n");
        return sb.toString();
    }

    public static String cleanTableName(String tableName) {
        tableName = tableName.replace("-", "");
        return tableName.toLowerCase();
    }

    public static String cleanFunctionName(String function) {
        if (function.length() < 63) {
            return function;
        }
        return "function_" + Math.abs(function.hashCode());
    }

}
