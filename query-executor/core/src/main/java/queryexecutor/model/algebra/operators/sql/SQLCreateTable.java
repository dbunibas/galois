package queryexecutor.model.algebra.operators.sql;

import java.util.List;
import java.util.Set;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.algebra.operators.ICreateTable;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.DBMSTable;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.utility.DBMSUtility;
import queryexecutor.utility.QueryExecutorUtility;

import static java.lang.Boolean.TRUE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLCreateTable implements ICreateTable {

    private static Logger logger = LoggerFactory.getLogger(SQLCreateTable.class);

    public void createTable(String tableName, List<Attribute> attributes, IDatabase target) {
        createTable(tableName, attributes, null, target);
    }

    public void createTable(String tableName, List<Attribute> attributes, Set<String> primaryKeys, IDatabase target) {
        AccessConfiguration accessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        StringBuilder sb = new StringBuilder();
        sb.append("create table ").append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(tableName).append("(\n");
        if (!containsOID(attributes)) {
            sb.append(QueryExecutorConstants.INDENT).append(QueryExecutorConstants.OID).append(" serial,\n");
        }
        for (Attribute attribute : attributes) {
            String attributeName = attribute.getName();
            String attributeType = attribute.getType();
            sb.append(QueryExecutorConstants.INDENT)
                    .append(attributeName)
                    .append(" ")
                    .append(DBMSUtility.convertDataSourceTypeToDBType(attributeType))
                    .append(TRUE.equals(attribute.getNullable()) ? "" : " NOT NULL" )
                    .append(",\n");
        }
        QueryExecutorUtility.removeChars(",\n".length(), sb);
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            sb.append(", PRIMARY KEY(").append(String.join(", ", primaryKeys)).append(")");
        }
//        sb.append(") with oids;");
        sb.append(");");
        logger.debug("Create Table Script: \n" + sb.toString());
        QueryManager.executeScript(sb.toString(), accessConfiguration, true, true, false, false);
        DBMSTable table = new DBMSTable(tableName, accessConfiguration);
        ((DBMSDB) target).addTable(table);
    }


    private boolean containsOID(List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(QueryExecutorConstants.OID)) {
                return true;
            }
        }
        return false;
    }
}
