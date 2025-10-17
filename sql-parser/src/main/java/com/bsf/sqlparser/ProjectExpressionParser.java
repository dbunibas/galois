package com.bsf.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.algebra.ProjectionAttribute;
import queryexecutor.model.algebra.aggregatefunctions.*;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.VirtualAttributeRef;
import queryexecutor.model.expressions.ExpressionAttributeRef;
import queryexecutor.persistence.Types;

import static com.bsf.sqlparser.ParseUtils.contextToParseContext;

@Slf4j
public class ProjectExpressionParser extends ExpressionVisitorAdapter<ProjectionAttribute> {
    @Override
    public <S> ProjectionAttribute visit(Column column, S context) {
        ParseContext parseContext = contextToParseContext(context);
        AttributeRef attributeRef = new AttributeRef(parseContext.getTableAliasFromColumn(column), column.getColumnName());
        return new ProjectionAttribute(attributeRef);
    }

    @Override
    public <S> ProjectionAttribute visit(AllTableColumns allTableColumns, S context) {
        ParseContext parseContext = contextToParseContext(context);
        AttributeRef attributeRef = new AttributeRef(parseContext.getTableAliasFromTable(allTableColumns.getTable()), ParseConstants.ALL_ATTRIBUTES);
        return new ProjectionAttribute(attributeRef);
    }

    @Override
    public <S> ProjectionAttribute visit(Function function, S context) {
        ParseContext parseContext = contextToParseContext(context);
        return switch (function.getName().toLowerCase()) {
            case "count" -> parseCount(function, parseContext);
            case "avg" -> parseAggregateFunction(AvgAggregateFunction::new, function, parseContext);
            case "min" -> parseAggregateFunction(MinAggregateFunction::new, function, parseContext);
            case "max" -> parseAggregateFunction(MaxAggregateFunction::new, function, parseContext);
            case "stddev" -> parseAggregateFunction(StdDevAggregateFunction::new, function, parseContext);
            case "sum" -> parseAggregateFunction(SumAggregateFunction::new, function, parseContext);
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
        ParseContext parseContext = contextToParseContext(context);
        Expression leftExpression = subtraction.getLeftExpression();
        Expression rightExpression = subtraction.getRightExpression();

        // TODO: Change ExpressionAttributeRef so that an expression with two tables (t1.a - t2.b) is allowed
        if (!(leftExpression instanceof Column) || !leftExpression.getClass().equals(rightExpression.getClass())) {
            throw new UnsupportedOperationException("Cannot define a subtraction projection between attributes of different tables");
        }

        String sqlExpressionWithoutAliases = String.format(
                "%s %s %s",
                expressionToValue(leftExpression),
                subtraction.getStringExpression(),
                expressionToValue(rightExpression)
        );
        String jepExpression = sqlToJEPExpressionString(sqlExpressionWithoutAliases);
        log.debug("Subtraction jepExpression {}", jepExpression);
        queryexecutor.model.expressions.Expression expression = new queryexecutor.model.expressions.Expression(jepExpression);
        setVariableDescription(leftExpression, expression, parseContext);
        setVariableDescription(rightExpression, expression, parseContext);
        ExpressionAttributeRef expressionAttributeRef = new ExpressionAttributeRef(
                expression,
                parseContext.getTableAliasFromColumn((Column) leftExpression),
                "diff",
                Types.REAL
        );
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

    private void setVariableDescription(Expression expression, queryexecutor.model.expressions.Expression executorExpression, ParseContext parseContext) {
        if (!(expression instanceof Column column)) {
            return;
        }
        String name = column.getColumnName();
        AttributeRef attributeRef = new AttributeRef(parseContext.getTableAliasFromColumn(column), name);
        executorExpression.setVariableDescription(name, attributeRef);
    }

    private ProjectionAttribute parseCount(Function function, ParseContext parseContext) {
        Expression exp = function.getParameters().getFirst();
        String attributeName = exp instanceof Column ? ((Column) exp).getColumnName() : QueryExecutorConstants.COUNT;
        TableAlias tableAlias = exp instanceof Column ?
                parseContext.getTableAliasFromColumn((Column) exp) :
                parseContext.getTableAliases().getFirst();
        AttributeRef attributeCount = new VirtualAttributeRef(tableAlias, attributeName, Types.INTEGER);
        return new ProjectionAttribute(new CountAggregateFunction(attributeCount));
    }

    private ProjectionAttribute parseAggregateFunction(AggregateFunctionSupplier supplier, Function function, ParseContext parseContext) {
        if (function.getParameters() == null || function.getParameters().size() != 1 || !(function.getParameters().getFirst() instanceof Column column)) {
            throw new UnsupportedOperationException("Cannot parse aggregate function without a single parameter!");
        }

        AttributeRef attributeRef = new VirtualAttributeRef(
                parseContext.getTableAliasFromColumn(column),
                column.getColumnName(),
                Types.REAL
        );
        return new ProjectionAttribute(supplier.get(attributeRef));
    }

    @FunctionalInterface
    private interface AggregateFunctionSupplier {
        IAggregateFunction get(AttributeRef attributeRef);
    }
}
