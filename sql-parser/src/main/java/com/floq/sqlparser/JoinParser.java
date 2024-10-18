package com.floq.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.Join;
import engine.model.database.AttributeRef;
import engine.model.database.TableAlias;

import java.util.List;

@Slf4j
public class JoinParser {
    public engine.model.algebra.Join parseJoin(Join join, TableAlias leftTableAlias, TableAlias rightTableAlias) {
        if (join.getOnExpressions().size() > 1) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot parse join with multiple on expressions (size %d)",
                            join.getOnExpressions().size()
                    )
            );
        }

        Expression expression = join.getOnExpressions().stream().findFirst().orElseThrow();
        JoinExpressionParser joinExpressionParser = new JoinExpressionParser();
        ParseContext parseContext = new ParseContext(leftTableAlias, rightTableAlias);
        JoinAttributeRefs joinAttributeRefs = expression.accept(joinExpressionParser, parseContext);

        return new engine.model.algebra.Join(joinAttributeRefs.leftAttributes(), joinAttributeRefs.rightAttributes());
    }

    private static class JoinExpressionParser extends ExpressionVisitorAdapter<JoinAttributeRefs> {
        @Override
        public <S> JoinAttributeRefs visit(EqualsTo equalsTo, S context) {
            ParseContext parseContext = (ParseContext) context;
            AttributeRef leftAttribute = parseColumn(parseContext.leftTableAlias(), equalsTo.getLeftExpression(Column.class));
            AttributeRef rightAttribute = parseColumn(parseContext.rightTableAlias(), equalsTo.getRightExpression(Column.class));
            return new JoinAttributeRefs(List.of(leftAttribute), List.of(rightAttribute));
        }

        private AttributeRef parseColumn(TableAlias tableAlias, Column column) {
            return new AttributeRef(tableAlias, column.getColumnName());
        }
    }

    private record ParseContext(TableAlias leftTableAlias, TableAlias rightTableAlias) {
    }

    private record JoinAttributeRefs(List<AttributeRef> leftAttributes, List<AttributeRef> rightAttributes) {
    }
}
