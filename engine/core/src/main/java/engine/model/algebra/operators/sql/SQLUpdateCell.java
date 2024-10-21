package engine.model.algebra.operators.sql;

import engine.EngineConstants;
import engine.model.algebra.operators.IUpdateCell;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.CellRef;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.dbms.DBMSDB;
import engine.persistence.Types;
import engine.persistence.relational.QueryManager;
import engine.utility.EngineUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.utility.DBMSUtility;

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
        Attribute attribute = EngineUtility.getAttribute(attributeRef, database);
        if(attribute.getType().equals(Types.STRING)){
            query.append("'");
        }
        query.append(cleanValue(value.toString()));
        if(attribute.getType().equals(Types.STRING)){
            query.append("'");
        }
        query.append(" WHERE ").append(EngineConstants.OID).append("=");
        query.append(cellRef.getTupleOID());
        query.append(";");
        if (logger.isDebugEnabled()) logger.debug("Update script: \n" + query.toString());
        QueryManager.executeScript(query.toString(), ((DBMSDB) database).getAccessConfiguration(), true, true, false, false);
    }
    
    private String cleanValue(String string) {
        String sqlValue = string;
        sqlValue = sqlValue.replaceAll("'", "''");
        sqlValue = EngineUtility.cleanConstantValue(sqlValue);
        return sqlValue;
    }
}
