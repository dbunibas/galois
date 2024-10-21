package engine.model.database.dbms;

import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.Attribute;
import engine.model.database.ITable;
import engine.model.database.OidTupleComparator;
import engine.model.database.Tuple;
import engine.EngineConstants;
import engine.exceptions.DBMSException;
import engine.persistence.relational.AccessConfiguration;
import engine.utility.DBMSUtility;
import engine.persistence.relational.QueryManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import engine.model.database.operators.lazyloading.DBMSTupleLoaderIterator;
import engine.model.database.operators.lazyloading.ITupleLoader;
import engine.utility.EngineUtility;

public class DBMSVirtualTable implements ITable {

    private final String tableName;
    private String suffix;
    private AccessConfiguration accessConfiguration;
    private List<Attribute> attributes;
//    private final ITable originalTable;

    public DBMSVirtualTable(ITable originalTable, AccessConfiguration accessConfiguration, String suffix) {
        this.tableName = originalTable.getName();
        this.accessConfiguration = accessConfiguration;
        this.suffix = suffix;
//        this.originalTable = originalTable;
//        initConnection();
    }

    public String getName() {
        return this.tableName;
    }

    public String getSuffix() {
        return suffix;
    }

    public List<Attribute> getAttributes() {
        if (attributes == null) {
            initConnection();
        }
        return attributes;
    }

    public Attribute getAttribute(String name) {
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Table " + tableName + " doesn't contain attribute " + name);
    }

    public ITupleIterator getTupleIterator() {
        ResultSet resultSet = DBMSUtility.getTableResultSetSortByOID(getVirtualName(), accessConfiguration);
        return new DBMSTupleIterator(resultSet, tableName);
    }

    public ITupleIterator getTupleIterator(int offset, int limit) {
        String query = getPaginationQuery(offset, limit);
        ResultSet resultSet = QueryManager.executeQuery(query, accessConfiguration);
        return new DBMSTupleIterator(resultSet, tableName);
    }

    public String getPaginationQuery(int offset, int limit) {
        return DBMSUtility.createTablePaginationQuery(getVirtualName(), accessConfiguration, offset, limit);
    }

    public Iterator<ITupleLoader> getTupleLoaderIterator() {
        ResultSet resultSet = DBMSUtility.getTableOidsResultSet(getVirtualName(), accessConfiguration);
        return new DBMSTupleLoaderIterator(resultSet, tableName, getVirtualName(), accessConfiguration);
    }

    public String printSchema(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("VirtualTable: ").append(toShortString()).append("{\n");
        for (Attribute attribute : getAttributes()) {
            result.append(indent).append(EngineConstants.INDENT);
            result.append(attribute.getName()).append(" ");
            result.append(attribute.getType()).append("\n");
        }
        result.append(indent).append("}\n");
        return result.toString();
    }

    public String toString() {
        return toString("");
    }

    public String toShortString() {
        return DBMSUtility.getSchemaNameAndDot(accessConfiguration) + this.getVirtualName();
    }

    public String getVirtualName() {
        return tableName + suffix;
    }

    public AccessConfiguration getAccessConfiguration() {
        return accessConfiguration;
    }

    public String toString(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("VirtualTable: ").append(toShortString()).append("{\n");
        ITupleIterator iterator = getTupleIterator();
        while (iterator.hasNext()) {
            result.append(indent).append(EngineConstants.INDENT).append(iterator.next()).append("\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    public String toStringWithSort(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append(" {\n");
        ITupleIterator iterator = getTupleIterator();
        List<Tuple> tuples = new ArrayList<Tuple>();
        while (iterator.hasNext()) {
            tuples.add(iterator.next());
        }
        Collections.sort(tuples, new OidTupleComparator());
        for (Tuple tuple : tuples) {
            result.append(indent).append(EngineConstants.INDENT).append(tuple.toString()).append("\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    private void initConnection() {
        ResultSet resultSet = null;
        try {
            resultSet = DBMSUtility.getTableResultSetForSchema(getVirtualName(), accessConfiguration);
            this.attributes = DBMSUtility.getTableAttributes(resultSet, tableName);
        } catch (SQLException ex) {
            throw new DBMSException("Unable to load table " + tableName + ".\n" + ex);
        } finally {
            QueryManager.closeResultSet(resultSet);
        }
    }

    public long getSize() {
        String query = "SELECT count(*) as count FROM " + accessConfiguration.getSchemaAndSuffix() + "." + getVirtualName();
        ResultSet resultSet = null;
        try {
            resultSet = QueryManager.executeQuery(query, accessConfiguration);
            resultSet.next();
            return resultSet.getLong("count");
        } catch (SQLException ex) {
            throw new DBMSException("Unable to execute query " + query + " on database \n" + accessConfiguration + "\n" + ex);
        } finally {
            QueryManager.closeResultSet(resultSet);
        }
    }

    public long getNumberOfDistinctTuples() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT count(*) as count FROM (");
        query.append(" SELECT DISTINCT ");
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().equalsIgnoreCase(EngineConstants.OID)) {
                continue;
            }
            query.append(attribute.getName()).append(", ");
        }
        EngineUtility.removeChars(", ".length(), query);
        query.append(" FROM ").append(accessConfiguration.getSchemaAndSuffix()).append(".").append(getVirtualName());
        query.append(") AS tmp");
        ResultSet resultSet = null;
        try {
            resultSet = QueryManager.executeQuery(query.toString(), accessConfiguration);
            resultSet.next();
            return resultSet.getLong("count");
        } catch (SQLException ex) {
            throw new DBMSException("Unable to execute query " + query + " on database \n" + accessConfiguration + "\n" + ex);
        } finally {
            QueryManager.closeResultSet(resultSet);
        }
    }
}
