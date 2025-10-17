package queryexecutor.model.database.dbms;

import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.OidTupleComparator;
import queryexecutor.model.database.Tuple;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DBMSException;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.utility.DBMSUtility;
import queryexecutor.persistence.relational.QueryManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import queryexecutor.model.database.operators.lazyloading.DBMSTupleLoaderIterator;
import queryexecutor.model.database.operators.lazyloading.ITupleLoader;
import queryexecutor.utility.QueryExecutorUtility;

public class DBMSTable implements ITable {

    private String tableName;
    private AccessConfiguration accessConfiguration;
    private List<Attribute> attributes;
    private Long size;

    public DBMSTable(String name, AccessConfiguration accessConfiguration) {
        this.tableName = name;
        this.accessConfiguration = accessConfiguration;
    }

    public String getName() {
        return this.tableName;
    }

    public List<Attribute> getAttributes() {
        if (attributes == null) {
            initConnection();
        }
        return attributes;
    }

    public Attribute getAttribute(String name) {
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().equalsIgnoreCase(name)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Table " + tableName + " doesn't contain attribute " + name + ". Attributes " + QueryExecutorUtility.printCollection(attributes));
    }

    public ITupleIterator getTupleIterator(int offset, int limit) {
        String query = getPaginationQuery(offset, limit);
        ResultSet resultSet = QueryManager.executeQuery(query, accessConfiguration);
        return new DBMSTupleIterator(resultSet, tableName);
    }

    public String getPaginationQuery(int offset, int limit) {
        return DBMSUtility.createTablePaginationQuery(tableName, accessConfiguration, offset, limit);
    }

    public ITupleIterator getTupleIterator() {
        ResultSet resultSet = DBMSUtility.getTableResultSetSortByOID(tableName, accessConfiguration);
        return new DBMSTupleIterator(resultSet, tableName);
    }

    public long getSize() {
        if (size == null) {
            size = getSizeNoCache();
        }
        return size;
    }

    public long getSizeNoCache() {
        String query = "SELECT count(*) as count FROM " + DBMSUtility.getSchemaNameAndDot(accessConfiguration) + tableName;
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
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(QueryExecutorConstants.OID)) {
                continue;
            }
            query.append(attribute.getName()).append(", ");
        }
        QueryExecutorUtility.removeChars(", ".length(), query);
        query.append(" FROM ").append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(tableName);
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

    public Iterator<ITupleLoader> getTupleLoaderIterator() {
        ResultSet resultSet = DBMSUtility.getTableOidsResultSet(tableName, accessConfiguration);
        return new DBMSTupleLoaderIterator(resultSet, tableName, accessConfiguration);
    }

    public AccessConfiguration getAccessConfiguration() {
        return accessConfiguration;
    }

    public void setAccessConfiguration(AccessConfiguration accessConfiguration) {
        this.accessConfiguration = accessConfiguration;
    }

    public String printSchema(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append("{\n");
        for (Attribute attribute : getAttributes()) {
            result.append(indent).append(QueryExecutorConstants.INDENT);
            result.append(attribute.getName()).append(" ");
            result.append(attribute.getType()).append("\n");
        }
        result.append(indent).append("}\n");
        return result.toString();
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toShortString() {
        return DBMSUtility.getSchemaNameAndDot(accessConfiguration) + this.tableName;
    }

    public String toString(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append("{\n");
        ITupleIterator iterator = getTupleIterator();
        while (iterator.hasNext()) {
            result.append(indent).append(QueryExecutorConstants.INDENT).append(iterator.next()).append("\n");
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
            result.append(indent).append(QueryExecutorConstants.INDENT).append(tuple.toString()).append("\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    private void initConnection() {
//        ResultSet resultSet = null;
//        try {
//            resultSet = DBMSUtility.getTableResultSetForSchema(tableName, accessConfiguration);
//            this.attributes = DBMSUtility.getTableAttributes(resultSet, tableName);
//        } catch (SQLException ex) {
//            throw new DBMSException("Unable to load table " + tableName + ".\n" + ex);
//        } finally {
//            QueryManager.closeResultSet(resultSet);
//        }
        this.attributes = DBMSUtility.loadAttributesFromTable(tableName, accessConfiguration);
    }

    public void reset() {
        this.size = null;
        this.attributes = null;
    }
}
