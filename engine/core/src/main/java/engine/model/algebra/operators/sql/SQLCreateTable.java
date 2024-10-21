package engine.model.algebra.operators.sql;

import java.util.List;
import java.util.Set;

import engine.EngineConstants;
import engine.model.algebra.operators.ICreateTable;
import engine.model.database.Attribute;
import engine.model.database.IDatabase;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.dbms.DBMSTable;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

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
            sb.append(EngineConstants.INDENT).append(EngineConstants.OID).append(" serial,\n");
        }
        for (Attribute attribute : attributes) {
            String attributeName = attribute.getName();
            String attributeType = attribute.getType();
            sb.append(EngineConstants.INDENT)
                    .append(attributeName)
                    .append(" ")
                    .append(DBMSUtility.convertDataSourceTypeToDBType(attributeType))
                    .append(TRUE.equals(attribute.getNullable()) ? "" : " NOT NULL" )
                    .append(",\n");
        }
        EngineUtility.removeChars(",\n".length(), sb);
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
            if (attribute.getName().equalsIgnoreCase(EngineConstants.OID)) {
                return true;
            }
        }
        return false;
    }
}
