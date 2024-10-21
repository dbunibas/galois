package engine.model.algebra.operators.sql;

import engine.utility.EngineUtility;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Scan;
import engine.model.algebra.Select;
import engine.model.algebra.operators.IDelete;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;
import engine.model.database.dbms.DBMSDB;
import engine.model.expressions.Expression;
import engine.utility.DBMSUtility;
import engine.persistence.relational.QueryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.persistence.relational.AccessConfiguration;

public class SQLDelete implements IDelete {

    private static Logger logger = LoggerFactory.getLogger(SQLDelete.class);
    private ExpressionToSQL sqlGenerator = new ExpressionToSQL();

    public boolean execute(String tableName, IAlgebraOperator operator, IDatabase source, IDatabase target) {
        StringBuilder deleteQuery = new StringBuilder();
        deleteQuery.append("DELETE FROM ");
        if (operator == null) {
            deleteQuery.append(tableAliasToSQL(new TableAlias(tableName), source, target, ((DBMSDB) target).getAccessConfiguration()));
        } else {
            deleteQuery.append(getScanQuery(operator, source, target));
        }
        deleteQuery.append(getSelectQuery(operator, source, target));
        deleteQuery.append(";");
        if (logger.isDebugEnabled()) logger.debug("Delete query:\n" + deleteQuery.toString());
        int affectedTuples = QueryManager.executeInsertOrDelete(deleteQuery.toString(), ((DBMSDB) target).getAccessConfiguration());
        return affectedTuples > 0;
    }

    private String getScanQuery(IAlgebraOperator operator, IDatabase source, IDatabase target) {
        if (operator instanceof Scan) {
            TableAlias tableAlias = ((Scan) operator).getTableAlias();
            return tableAliasToSQL(tableAlias, source, target, ((DBMSDB) target).getAccessConfiguration());
        }
        for (IAlgebraOperator child : operator.getChildren()) {
            return getScanQuery(child, source, target);
        }
        throw new IllegalArgumentException("Unable to create delete query from " + operator);
    }

    private String getSelectQuery(IAlgebraOperator operator, IDatabase source, IDatabase target) {
        if (operator == null) {
            return "";
        }
        if (operator instanceof Select) {
            StringBuilder result = new StringBuilder();
            result.append(" WHERE ");
            for (Expression condition : ((Select) operator).getSelections()) {
                result.append(sqlGenerator.expressionToSQL(condition, source, target));
                result.append(" AND ");
            }
            EngineUtility.removeChars(" AND ".length(), result);
            return result.toString();
        }
        for (IAlgebraOperator child : operator.getChildren()) {
            return getSelectQuery(child, source, target);
        }
        return "";
    }

    private String tableAliasToSQL(TableAlias tableAlias, IDatabase source, IDatabase target, AccessConfiguration configuration) {
        StringBuilder sb = new StringBuilder();
        if (DBMSUtility.supportsSchema(configuration)) {
            if (tableAlias.isSource()) {
                String sourceSchemaName = "source";
                if (source != null && source instanceof DBMSDB) {
                    sourceSchemaName = DBMSUtility.getSchemaNameAndDot(((DBMSDB) source).getAccessConfiguration());
                }
                sb.append(sourceSchemaName);
            } else {
                String targetSchemaName = "target";
                if (target != null && target instanceof DBMSDB) {
                    targetSchemaName = DBMSUtility.getSchemaNameAndDot(((DBMSDB) target).getAccessConfiguration());
                }
                sb.append(targetSchemaName);
            }
        }
        sb.append(tableAlias.getTableName());
        if (tableAlias.isAliased()) {
            sb.append(" AS ").append(DBMSUtility.tableAliasToSQL(tableAlias));
        }
        return sb.toString();
    }
}
