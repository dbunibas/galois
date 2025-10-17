package queryexecutor.model.algebra.operators.sql;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.algebra.operators.IUpdateCell;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.CellRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.persistence.Types;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.utility.QueryExecutorUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.DBMSUtility;

public class SQLUpdateCell implements IUpdateCell {
    
    private static Logger logger = LoggerFactory.getLogger(SQLUpdateCell.class);
    
    @Override
    public void execute(CellRef cellRef, IValue value, IDatabase database) {
        if (logger.isDebugEnabled()) logger.debug("Changing cell " + cellRef + " with new value " + value + " in database " + database);
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        AttributeRef attributeRef = cellRef.getAttributeRef();
        query.append(DBMSUtility.getSchemaNameAndDot(((DBMSDB) database).getAccessConfiguration()));
        query.append(cellRef.getAttributeRef().getTableName());
        query.append(" SET ").append(attributeRef.getName()).append("=");
        Attribute attribute = QueryExecutorUtility.getAttribute(attributeRef, database);
        if(attribute.getType().equals(Types.STRING)){
            query.append("'");
        }
        query.append(cleanValue(value.toString()));
        if(attribute.getType().equals(Types.STRING)){
            query.append("'");
        }
        query.append(" WHERE ").append(QueryExecutorConstants.OID).append("=");
        query.append(cellRef.getTupleOID());
        query.append(";");
        if (logger.isDebugEnabled()) logger.debug("Update script: \n" + query.toString());
        QueryManager.executeScript(query.toString(), ((DBMSDB) database).getAccessConfiguration(), true, true, false, false);
    }
    
    private String cleanValue(String string) {
        String sqlValue = string;
        sqlValue = sqlValue.replaceAll("'", "''");
        sqlValue = QueryExecutorUtility.cleanConstantValue(sqlValue);
        return sqlValue;
    }
}
