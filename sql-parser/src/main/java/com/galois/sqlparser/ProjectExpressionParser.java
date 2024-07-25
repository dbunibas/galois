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
import speedy.persistence.Types;

import java.util.List;

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
