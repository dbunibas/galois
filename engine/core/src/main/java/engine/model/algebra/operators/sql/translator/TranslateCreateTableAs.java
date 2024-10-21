package engine.model.algebra.operators.sql.translator;

import java.util.ArrayList;
import engine.EngineConstants;
import engine.model.algebra.CreateTableAs;
import engine.model.algebra.Distinct;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Join;
import engine.model.algebra.Project;

public class TranslateCreateTableAs {

    public void translate(CreateTableAs operator, AlgebraTreeToSQLVisitor visitor) {
        String currentResult = visitor.getSQLQueryBuilder().toString();
        visitor.setSQLQueryBuilder(new SQLQueryBuilder());
        String tableName = operator.getTableName();
        if (operator.getFather() != null) {
            visitor.setCounter(visitor.getCounter() + 1);
            tableName += "_" + visitor.getCounter();
        }
        IAlgebraOperator child = operator.getChildren().get(0);
        visitor.getSQLQueryBuilder().append("DROP TABLE IF EXISTS ").append(operator.getSchemaName()).append(".").append(tableName).append(";\n");
        visitor.getSQLQueryBuilder().append("CREATE " + (operator.isUnlogged() ? " UNLOGGED " : "") + "TABLE ").append(operator.getSchemaName()).append(".").append(tableName);
        if (operator.isWithOIDs()) {
            if (child instanceof Distinct) {
                visitor.getSQLQueryBuilder().append(" WITH oids ");
            } else {
                visitor.setAddOIDColumn(true);
            }
        }
        visitor.getSQLQueryBuilder().append(" AS (\n");
        visitor.incrementIndentLevel();
        child.accept(visitor);
        visitor.reduceIndentLevel();
        visitor.getSQLQueryBuilder().append(");").append("\n");
        String createTableQuery = visitor.getSQLQueryBuilder().toString();
        visitor.getCreateTableQueries().add(createTableQuery);
        if (tableName.startsWith(EngineConstants.DELTA_TMP_TABLES)) {
            visitor.getDropTempTableQueries().add("DROP TABLE IF EXISTS " + operator.getSchemaName() + "." + tableName + ";\n");
        }
        visitor.setSQLQueryBuilder(new SQLQueryBuilder(currentResult));
        if (operator.getFather() != null) {
            if (operator.getFather() instanceof Join) {
                visitor.getSQLQueryBuilder().append(operator.getSchemaName()).append(".").append(tableName).append(" AS ").append(operator.getTableAlias());
            } else if (operator.getFather() instanceof Project) {
                visitor.createSQLSelectClause(operator, new ArrayList<NestedOperator>(), false);
                visitor.getSQLQueryBuilder().append(" FROM ").append(operator.getSchemaName()).append(".").append(tableName).append(" AS ").append(operator.getTableAlias());
            } else {
                throw new IllegalArgumentException("Create table is allowed only on Join or Project");
            }
        }
    }

}
