package com.galois.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import speedy.SpeedyConstants;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.algebra.aggregatefunctions.*;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.database.VirtualAttributeRef;
import speedy.model.expressions.ExpressionAttributeRef;
import speedy.persistence.Types;

@Slf4j
public class ProjectExpressionParser extends ExpressionVisitorAdapter<ProjectionAttribute> {
    @Override
    public <S> ProjectionAttribute visit(Column column, S context) {
        AttributeRef attributeRef = new AttributeRef((TableAlias) context, column.getColumnName());
        return new ProjectionAttribute(attributeRef);
    }

    @Override
    public <S> ProjectionAttribute visit(Function function, S context) {
        TableAlias tableAlias = contextToTableAlias(context);
        return switch (function.getName()) {
            case "count" -> parseCount(function, tableAlias);
            case "avg" -> parseAggregateFunction(AvgAggregateFunction::new, function, tableAlias);
            case "min" -> parseAggregateFunction(MinAggregateFunction::new, function, tableAlias);
            case "max" -> parseAggregateFunction(MaxAggregateFunction::new, function, tableAlias);
            case "stddev" -> parseAggregateFunction(StdDevAggregateFunction::new, function, tableAlias);
            case "sum" -> parseAggregateFunction(SumAggregateFunction::new, function, tableAlias);
            default -> throw new UnsupportedOperationException(String.format(
                    "Function %s is currently unsupported...",
                    function.getName()
            ));
        };
    }

    @Override
    public <S> ProjectionAttribute visit(ExpressionList<? extends Expression> expressionList, S context) {
        if (expressionList.size() > 1) {
            throw new UnsupportedOperationException(String.format("Cannot parse project expression with size %d", expressionList.size()));
        }
        Expression expression = expressionList.getFirst();
        ProjectionAttribute projectionAttribute = expression.accept(this, context);
        if (projectionAttribute == null) {
            throw new UnsupportedOperationException(String.format("Cannot parse expression %s", expression.getClass()));
        }
        return projectionAttribute;
    }


    // FIXME: Refactor using a common expression parser (similar to WhereParser.parseBinaryExpression)
    @Override
    public <S> ProjectionAttribute visit(Subtraction subtraction, S context) {
        TableAlias tableAlias = contextToTableAlias(context);
        Expression leftExpression = subtraction.getLeftExpression();
        Expression rightExpression = subtraction.getRightExpression();
        String sqlExpressionWithoutAliases = String.format(
                "%s %s %s",
                expressionToValue(leftExpression),
                subtraction.getStringExpression(),
                expressionToValue(rightExpression)
        );
        String jepExpression = sqlToJEPExpressionString(sqlExpressionWithoutAliases);
        log.debug("Subtraction jepExpression {}", jepExpression);
        speedy.model.expressions.Expression expression = new speedy.model.expressions.Expression(jepExpression);
        setVariableDescription(leftExpression, expression, tableAlias);
        setVariableDescription(rightExpression, expression, tableAlias);
        ExpressionAttributeRef expressionAttributeRef = new ExpressionAttributeRef(expression, tableAlias, "diff", Types.REAL);
        return new ProjectionAttribute(expressionAttributeRef);
    }

    private Object expressionToValue(net.sf.jsqlparser.expression.Expression expression) {
        return expression instanceof Column ? ((Column) expression).getColumnName() : expression.toString();
    }

    private String sqlToJEPExpressionString(String sqlExpression) {
        return sqlExpression
                .replaceAll("'", "\"")
                // Spaces make sure '=' is not replaced when used in conjunction with '>' o '<'
                .replaceAll(" = ", " == ")
                .replaceAll("<>", "!=");
    }

    private void setVariableDescription(Expression expression, speedy.model.expressions.Expression speedyExpression, TableAlias tableAlias) {
        if (!(expression instanceof Column column)) {
            return;
        }
        String name = column.getColumnName();
        AttributeRef attributeRef = new AttributeRef(tableAlias, name);
        speedyExpression.setVariableDescription(name, attributeRef);
    }

    private <S> TableAlias contextToTableAlias(S context) {
        if (!(context instanceof TableAlias)) {
            throw new IllegalArgumentException("Cannot parse select item without table alias!");
        }
        return (TableAlias) context;
    }

    private ProjectionAttribute parseCount(Function function, TableAlias tableAlias) {
        String attributeName = function.getParameters().getFirst() instanceof Column ?
                ((Column) function.getParameters().getFirst()).getColumnName() :
                SpeedyConstants.COUNT;
        AttributeRef attributeCount = new VirtualAttributeRef(tableAlias, attributeName, Types.INTEGER);
        return new ProjectionAttribute(new CountAggregateFunction(attributeCount));
    }

    private ProjectionAttribute parseAggregateFunction(AggregateFunctionSupplier supplier, Function function, TableAlias tableAlias) {
        if (function.getParameters() == null || function.getParameters().size() != 1 || !(function.getParameters().getFirst() instanceof Column)) {
            throw new UnsupportedOperationException("Cannot parse aggregate function without a single parameter!");
        }

        AttributeRef attributeRef = new VirtualAttributeRef(tableAlias, ((Column) function.getParameters().getFirst()).getColumnName(), Types.REAL);
        return new ProjectionAttribute(supplier.get(attributeRef));
    }

    @FunctionalInterface
    private interface AggregateFunctionSupplier {
        IAggregateFunction get(AttributeRef attributeRef);
    }
}
