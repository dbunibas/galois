package engine.model.algebra.operators.sql;

import org.apache.commons.lang.StringEscapeUtils;
import engine.utility.EngineUtility;
import engine.model.algebra.operators.IInsertTuple;
import engine.model.database.dbms.DBMSTable;
import engine.persistence.Types;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import engine.model.database.Attribute;
import engine.model.database.Cell;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;
import engine.utility.DBMSUtility;

public class SQLInsertTuple implements IInsertTuple {

    private static Logger logger = LoggerFactory.getLogger(SQLInsertTuple.class);

    public void execute(ITable table, Tuple tuple, IDatabase source, IDatabase target) {
        DBMSTable dbmsTable = (DBMSTable) table;
        StringBuilder insertQuery = buildInsertScript(dbmsTable, tuple, source, target);
        if (logger.isDebugEnabled()) logger.debug("Insert query:\n" + insertQuery.toString());
        QueryManager.executeInsertOrDelete(insertQuery.toString(), ((DBMSTable) table).getAccessConfiguration());
    }

    public StringBuilder buildInsertScript(DBMSTable dbmsTable, Tuple tuple, IDatabase source, IDatabase target) {
        AccessConfiguration accessConfiguration = dbmsTable.getAccessConfiguration();
        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("INSERT INTO ");
        insertQuery.append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(dbmsTable.getName());
        insertQuery.append(" (");
        for (Cell cell : tuple.getCells()) {
            insertQuery.append(cell.getAttribute()).append(", ");
        }
        EngineUtility.removeChars(", ".length(), insertQuery);
        insertQuery.append(")");
        insertQuery.append(" VALUES (");
        for (Cell cell : tuple.getCells()) {
            if (cell.getValue() == null || cleanValue(cell.getValue().toString()).equalsIgnoreCase(EngineConstants.NULL_VALUE)) {
                insertQuery.append("null, ");
                continue;
            }
            String cellValue = cell.getValue().toString();
            cellValue = cleanValue(cellValue);
            String attributeType = getAttributeType(dbmsTable, cell.getAttributeRef().getName());
            if (attributeType.equals(Types.INTEGER) && cellValue.isEmpty()) {
                cellValue = "null";
            }
            if (attributeType.equals(Types.STRING) || attributeType.equals(Types.DATE) || attributeType.equals(Types.DATETIME)) {
                insertQuery.append("'");
            }
            if (attributeType.equals(Types.STRING)) cellValue = StringEscapeUtils.escapeSql(cellValue);
            insertQuery.append(cellValue);
            if (attributeType.equals(Types.STRING) || attributeType.equals(Types.DATE) || attributeType.equals(Types.DATETIME)) {
                insertQuery.append("'");
            }
            insertQuery.append(", ");
        }
        EngineUtility.removeChars(", ".length(), insertQuery);
        insertQuery.append(");");
        return insertQuery;
    }

    private String cleanValue(String cellValue) {
        cellValue = cellValue.replaceAll("'", "''");
        cellValue = cellValue.replaceAll("\\\\", "\\\\\\\\");
        return cellValue;
    }

    private String getAttributeType(DBMSTable table, String attributeName) {
        for (Attribute attribute : table.getAttributes()) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                return attribute.getType();
            }
        }
        throw new IllegalArgumentException("Unable to find attribute " + attributeName + " into table " + table.printSchema(""));
    }
}
