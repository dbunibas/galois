package engine.model.algebra.operators.sql.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import engine.model.algebra.GroupBy;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Join;
import engine.model.algebra.Scan;
import engine.model.algebra.Select;
import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.model.algebra.operators.sql.ExpressionToSQL;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;
import engine.model.expressions.Expression;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class TranslateGroupBy {

    private final static Logger logger = LoggerFactory.getLogger(TranslateGroupBy.class);
    private ExpressionToSQL sqlGenerator = new ExpressionToSQL();

    public void translate(GroupBy operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase source = visitor.getSource();
        IDatabase target = visitor.getTarget();
        result.append(visitor.indentString());
        result.append("SELECT ");
        if (result.isDistinct()) {
            result.append("DISTINCT ");
            result.setDistinct(false);
        }
        List<NestedOperator> nestedTables = findNestedTablesForGroupBy(operator, visitor);
        List<IAggregateFunction> aggregateFunctions = operator.getAggregateFunctions();
        List<String> havingFunctions = extractHavingFunctions(operator);
        if (logger.isDebugEnabled()) logger.debug("Having functions:\n" + EngineUtility.printCollection(havingFunctions));
        for (IAggregateFunction aggregateFunction : aggregateFunctions) {
            AttributeRef attributeRef = aggregateFunction.getAttributeRef();
            if (attributeRef.toString().contains(EngineConstants.AGGR + "." + EngineConstants.COUNT)) {
                continue;
            }
            if (visitor.getCurrentProjectionAttribute() == null) {
                visitor.setCurrentProjectionAttribute(new ArrayList<AttributeRef>());
            }
            visitor.getCurrentProjectionAttribute().add(aggregateFunction.getAttributeRef());
            result.append(visitor.aggregateFunctionToString(aggregateFunction, aggregateFunction.getAttributeRef(), nestedTables)).append(", ");
        }
        EngineUtility.removeChars(", ".length(), result.getStringBuilder());
        result.append("\n").append(visitor.indentString());
        result.append(" FROM ");
        IAlgebraOperator child = operator.getChildren().get(0);
        if (child instanceof Scan) {
            TableAlias tableAlias = ((Scan) child).getTableAlias();
            result.append(visitor.tableAliasToSQL(tableAlias));
        } else if (child instanceof Select) {
            Select select = (Select) child;
            visitSelectForGroupBy(select, visitor);
        } else if (child instanceof Join) {
            Join join = (Join) child;
            List<NestedOperator> nestedTablesForJoin = visitor.findNestedTablesForJoin(join);
            IAlgebraOperator leftChild = join.getChildren().get(0);
            IAlgebraOperator rightChild = join.getChildren().get(1);
            visitor.createJoinClause(join, leftChild, rightChild, nestedTablesForJoin);
        } else if (child instanceof GroupBy) {
            result.append("(\n");
            visitor.incrementIndentLevel();
            child.accept(visitor);
            visitor.reduceIndentLevel();
            result.append("\n").append(visitor.indentString()).append(") AS ");
//                result.append("Nest_").append(child.hashCode());
            result.append(child.getAttributes(source, target).get(0).getTableName());
        } else {
            throw new IllegalArgumentException("Group by not supported: " + operator);
        }
        result.append("\n").append(visitor.indentString());
        result.append(" GROUP BY ");
        for (AttributeRef groupingAttribute : operator.getGroupingAttributes()) {
//                result.append(DBMSUtility.attributeRefToSQLDot(groupingAttribute)).append(", ");
            if (visitor.containsAlias(nestedTables, groupingAttribute.getTableAlias())) {
                result.append(DBMSUtility.attributeRefToAliasSQL(groupingAttribute));
            } else {
                result.append(DBMSUtility.attributeRefToSQLDot(groupingAttribute));
            }
            result.append(", ");
        }
        EngineUtility.removeChars(", ".length(), result.getStringBuilder());
        if (!havingFunctions.isEmpty()) {
            result.append("\n").append(visitor.indentString());
            result.append(" HAVING ");
            for (String havingFunction : havingFunctions) {
                result.append(havingFunction).append(", ");
            }
            EngineUtility.removeChars(", ".length(), result.getStringBuilder());
        }
    }

    private void visitSelectForGroupBy(Select operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase source = visitor.getSource();
        IDatabase target = visitor.getTarget();
        List<Expression> expressionsToSelect = new ArrayList<Expression>();
        expressionsToSelect.addAll(operator.getSelections());
        IAlgebraOperator child = operator.getChildren().get(0);
        while (child instanceof Select) {
            Select selectChild = (Select) child;
            expressionsToSelect.addAll(selectChild.getSelections());
            child = selectChild.getChildren().get(0);
        }
        if (child instanceof Scan) {
            TableAlias tableAlias = ((Scan) child).getTableAlias();
            result.append(visitor.tableAliasToSQL(tableAlias));
        } else {
            throw new IllegalArgumentException("Group by not supported: " + operator);
        }
        result.append("\n").append(visitor.indentString());
        result.append(" WHERE  ");
        visitor.incrementIndentLevel();
        result.append("\n").append(visitor.indentString());
        for (Expression condition : expressionsToSelect) {
            result.append(sqlGenerator.expressionToSQL(condition, source, target));
            result.append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), result.getStringBuilder());
        visitor.reduceIndentLevel();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractHavingFunctions(GroupBy operator) {
        if (logger.isDebugEnabled()) logger.debug("GroupBy father: " + operator.getFather());
        if (!(operator.getFather() instanceof Select)) {
            return Collections.EMPTY_LIST;
        }
        Select select = (Select) operator.getFather();
        if (select.getSelections().size() != 1) {
            return Collections.EMPTY_LIST;
        }
        Expression expression = select.getSelections().get(0).clone();
        if (logger.isDebugEnabled()) logger.debug("Expression: " + expression);
        if (!expression.toString().contains(EngineConstants.AGGR + "." + EngineConstants.COUNT)) {
            throw new IllegalArgumentException("Having function " + expression + " is not yet supported!");
//            return Collections.EMPTY_LIST;
        }
        List<String> havingFunctions = new ArrayList<String>();
        if (expression.toString().contains("count")) {
            havingFunctions.add(getCountHavingSQL(expression));
        } else {
            throw new IllegalArgumentException("Having function " + expression + " is not yet supported!");
        }
        return havingFunctions;
    }

    private List<NestedOperator> findNestedTablesForGroupBy(GroupBy operator, AlgebraTreeToSQLVisitor visitor) {
        List<NestedOperator> tableAliases = new ArrayList<NestedOperator>();
        List<AttributeRef> attributes = new ArrayList<AttributeRef>();
        IAlgebraOperator child = operator.getChildren().get(0);
        attributes.addAll(visitor.getNestedAttributes(child));
        for (AttributeRef attributeRef : attributes) {
            if (visitor.containsAlias(tableAliases, attributeRef.getTableAlias())) {
                continue;
            }
            NestedOperator nestedOperator = new NestedOperator(operator, attributeRef.getTableAlias());
            tableAliases.add(nestedOperator);
        }
        return tableAliases;
    }

    private String getCountHavingSQL(Expression expression) {
        String variableName = expression.getVariables().get(0);
        String expressionString = expression.toString();
        expressionString = expressionString.replace(variableName, "count(*) ");
        return expressionString;
    }

}
