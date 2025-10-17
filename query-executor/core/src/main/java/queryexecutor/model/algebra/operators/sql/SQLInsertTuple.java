package queryexecutor.model.algebra.operators.sql;

import org.apache.commons.lang.StringEscapeUtils;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.model.algebra.operators.IInsertTuple;
import queryexecutor.model.database.dbms.DBMSTable;
import queryexecutor.persistence.Types;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;
import queryexecutor.utility.DBMSUtility;

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
        QueryExecutorUtility.removeChars(", ".length(), insertQuery);
        insertQuery.append(")");
        insertQuery.append(" VALUES (");
        for (Cell cell : tuple.getCells()) {
            if (cell.getValue() == null || cleanValue(cell.getValue().toString()).equalsIgnoreCase(QueryExecutorConstants.NULL_VALUE)) {
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
        QueryExecutorUtility.removeChars(", ".length(), insertQuery);
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
