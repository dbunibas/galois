package com.bsf.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import queryexecutor.model.algebra.GroupBy;
import queryexecutor.model.algebra.aggregatefunctions.IAggregateFunction;
import queryexecutor.model.algebra.aggregatefunctions.ValueAggregateFunction;
import queryexecutor.model.database.AttributeRef;

import java.util.List;

import static com.bsf.sqlparser.ParseUtils.contextToParseContext;

@Slf4j
public class GroupByParser implements GroupByVisitor<GroupBy> {
    @Override
    public <S> GroupBy visit(GroupByElement groupByElement, S context) {
        ExpressionList<?> groupByExpressionList = groupByElement.getGroupByExpressionList();
        GroupByExpressionParser groupByExpressionParser = new GroupByExpressionParser();
        List<ExpressionResult> expressionResults = groupByExpressionList.stream()
                .map(exp -> exp.accept(groupByExpressionParser, context))
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

    private static class GroupByExpressionParser extends ExpressionVisitorAdapter<ExpressionResult> {
        @Override
        public <S> ExpressionResult visit(Column column, S context) {
            ParseContext parseContext = contextToParseContext(context);
            AttributeRef attributeRef = new AttributeRef(parseContext.getTableAliasFromColumn(column), column.getColumnName());
            IAggregateFunction aggregateFunction = new ValueAggregateFunction(attributeRef);
            return new ExpressionResult(attributeRef, aggregateFunction);
        }
    }

    private record ExpressionResult(AttributeRef attributeRef, IAggregateFunction aggregateFunction) {}
}
