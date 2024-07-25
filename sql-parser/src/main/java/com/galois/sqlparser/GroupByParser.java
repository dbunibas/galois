package com.galois.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import speedy.model.algebra.GroupBy;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;

import java.util.List;

@Slf4j
public class GroupByParser implements GroupByVisitor<GroupBy> {
    @Override
    public <S> GroupBy visit(GroupByElement groupByElement, S context) {
        ExpressionList<?> groupByExpressionList = groupByElement.getGroupByExpressionList();
        GroupByExpressionParser groupByExpressionParser = new GroupByExpressionParser();
        List<ExpressionResult> expressionResults = groupByExpressionList.stream()
                .map(exp -> exp.accept(groupByExpressionParser, contextToTableAlias(context)))
                .toList();
        return new GroupBy(
                expressionResults.stream().map(ExpressionResult::attributeRef).toList(),
                expressionResults.stream().map(ExpressionResult::aggregateFunction).toList()
        );
    }

    @Override
    public void visit(GroupByElement groupBy) {
        throw new UnsupportedOperationException();
    }

    private <S> TableAlias contextToTableAlias(S context) {
        if (!(context instanceof TableAlias)) {
            throw new IllegalArgumentException("Cannot parse group by without table alias!");
        }
        return (TableAlias) context;
    }

    private static class GroupByExpressionParser extends ExpressionVisitorAdapter<ExpressionResult> {
        @Override
        public <S> ExpressionResult visit(Column column, S context) {
            TableAlias tableAlias = (TableAlias) context;
            AttributeRef attributeRef = new AttributeRef(tableAlias, column.getColumnName());
            IAggregateFunction aggregateFunction = new ValueAggregateFunction(attributeRef);
            return new ExpressionResult(attributeRef, aggregateFunction);
        }
    }

    private record ExpressionResult(AttributeRef attributeRef, IAggregateFunction aggregateFunction) {}
}
