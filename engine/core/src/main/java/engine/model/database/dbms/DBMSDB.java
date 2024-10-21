package engine.model.database.dbms;

import engine.EngineConstants;
import engine.model.database.ForeignKey;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Key;
import engine.model.database.operators.dbms.ExecuteInitDB;
import engine.persistence.relational.AccessConfiguration;
import engine.utility.DBMSUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class DBMSDB implements IDatabase {
    private static ExecuteInitDB initDBExecutor = new ExecuteInitDB();
    private AccessConfiguration accessConfiguration;
    private List<String> tableNames;
    private List<Key> keys;
    private List<Key> primaryKeys;
    private List<ForeignKey> foreignKeys;
    private boolean initialized = false;
    private List<DBMSTable> tables = new ArrayList<DBMSTable>();
    private InitDBConfiguration initDBConfiguration = new InitDBConfiguration();
    private Lock lock = new java.util.concurrent.locks.ReentrantLock();

    public DBMSDB(AccessConfiguration accessConfiguration) {
        this.accessConfiguration = accessConfiguration;
    }

    public void initDBMS() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!DBMSUtility.isDBExists(accessConfiguration)) {
            DBMSUtility.createDB(accessConfiguration);
        }
        if (!initDBConfiguration.isEmpty()
                && DBMSUtility.isSchemaEmpty(accessConfiguration)) {
            initDBExecutor.execute(this);
        }
        loadTables();
    }

    public void reset() {
        this.initialized = false;
        for (DBMSTable table : tables) {
            table.reset();
        }
    }

    public void loadTables() {
        this.lock.lock();
        try {
            for (String tableName : getTableNames()) {
                tables.add(new DBMSTable(tableName, accessConfiguration));
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void addTable(ITable table) {
        tables.add((DBMSTable) table);
        if (!getTableNames().contains(table.getName())) {
            tableNames.add(table.getName());
        }
    }

    public String getName() {
        return this.accessConfiguration.getSchemaAndSuffix();
    }

    public List<String> getTableNames() {
        initDBMS();
        if (tableNames == null) {
            tableNames = DBMSUtility.loadTableNames(accessConfiguration);
        }
        return tableNames;
    }

    public List<Key> getKeys() {
        initDBMS();
        if (keys == null) {
            keys = DBMSUtility.loadKeys(accessConfiguration);
        }
        return keys;
    }

    public List<Key> getKeys(String table) {
        List<Key> result = new ArrayList<Key>();
        for (Key key : getKeys()) {
            String tableName = key.getAttributes().get(0).getTableName();
            if (tableName.equals(table)) {
                result.add(key);
            }
        }
        return result;
    }

    @Override
    public List<Key> getPrimaryKeys() {
        initDBMS();
        if (primaryKeys == null) {
            primaryKeys = DBMSUtility.loadPrimaryKeys(accessConfiguration);
        }
        return primaryKeys;
    }

    public Key getPrimaryKey(String table) {
        for (Key key : getPrimaryKeys()) {
            String tableName = key.getAttributes().get(0).getTableName();
            if (tableName.equals(table)) {
                return key;
            }
        }
        return null;
    }

    public List<ForeignKey> getForeignKeys() {
        initDBMS();
        if (foreignKeys == null) {
            foreignKeys = DBMSUtility.loadForeignKeys(accessConfiguration);
        }
        return foreignKeys;
    }

    public List<ForeignKey> getForeignKeys(String table) {
        List<ForeignKey> result = new ArrayList<ForeignKey>();
        for (ForeignKey foreignKey : getForeignKeys()) {
            String tableName = foreignKey.getRefAttributes().get(0).getTableName();
            if (tableName.equals(table)) {
                result.add(foreignKey);
            }
        }
        return result;
    }

    public ITable getTable(String name) {
        initDBMS();
        if(tables.isEmpty()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        this.lock.lock();
        try {
//        return new DBMSTable(name, accessConfiguration);
            for (DBMSTable table : tables) {
                if (table.getName().equalsIgnoreCase(name.trim())) {
                    return table;
                }
            }
            StringBuilder error = new StringBuilder();
            error.append("Unable to find table ").append(name).append(" in database ").append(getName());
            error.append(" Tables (").append(tables.size()).append("):");
            for (DBMSTable table : tables) {
                error.append(table.getName()).append(", ");
            }
            throw new IllegalArgumentException(error.toString());
        } finally {
            this.lock.unlock();
        }
    }

    public ITable getFirstTable() {
        return getTable(getTableNames().get(0));
    }

    public AccessConfiguration getAccessConfiguration() {
        return accessConfiguration;
    }

    public InitDBConfiguration getInitDBConfiguration() {
        return initDBConfiguration;
    }

    public long getSize() {
        long size = 0;
        for (String tableName : tableNames) {
            DBMSTable table = (DBMSTable) getTable(tableName);
            size += table.getSize();
        }
        return size;
    }

    public long getSizeNoCache() {
        long size = 0;
        for (String tableName : tableNames) {
            DBMSTable table = (DBMSTable) getTable(tableName);
            size += table.getSizeNoCache();
        }
        return size;
    }

    public IDatabase clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String printSchema() {
        StringBuilder result = new StringBuilder();
        result.append(accessConfiguration).append("\n");
        result.append("Schema: ").append(getName()).append(" {\n");
        for (String tableName : getTableNames()) {
            DBMSTable table = (DBMSTable) getTable(tableName);
            result.append(table.printSchema(EngineConstants.INDENT));
//            table.closeConnection();
        }
        if (!getKeys().isEmpty()) {
            result.append(EngineConstants.INDENT).append("--------------- Keys: ---------------\n");
            for (Key key : getKeys()) {
                result.append(EngineConstants.INDENT).append(key).append("\n");
            }
        }
        if (!getForeignKeys().isEmpty()) {
            result.append(EngineConstants.INDENT).append("----------- Foreign Keys: -----------\n");
            for (ForeignKey foreignKey : getForeignKeys()) {
                result.append(EngineConstants.INDENT).append(foreignKey).append("\n");
            }
        }
        result.append("}\n");
        return result.toString();
    }

    public String printInstances() {
        return printInstances(false);
    }

    public String printInstances(boolean sort) {
        StringBuilder result = new StringBuilder();
        for (String tableName : getTableNames()) {
            DBMSTable table = (DBMSTable) getTable(tableName);
            if (sort) {
                result.append(table.toStringWithSort(EngineConstants.INDENT));
            } else {
                result.append(table.toString(EngineConstants.INDENT));
            }
//            table.closeConnection();
        }
        return result.toString();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(accessConfiguration).append("\n");
        result.append(printInstances());
        return result.toString();
    }
}
