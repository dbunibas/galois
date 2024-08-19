package com.galois.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class WhereParser extends ExpressionVisitorAdapter<WhereParser.WhereParseResult> {
    @Override
    public <S> WhereParseResult visit(EqualsTo exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(NotEqualsTo exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(GreaterThan exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(GreaterThanEquals exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(MinorThan exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(MinorThanEquals exp, S context) {
        return parseBinaryExpression(exp.getLeftExpression(), exp.getStringExpression(), exp.getRightExpression(), contextToTableAlias(context));
    }

    @Override
    public <S> WhereParseResult visit(IsNullExpression isNullExpression, S context) {
        Object value = expressionToValue(isNullExpression.getLeftExpression());
        String operator = isNullExpression.isNot() ? "isNotNull(%s)" : "isNull(%s)";
        Expression expression = new Expression(String.format(operator, value));
        VariableDescription variableDescription = setVariableDescription(isNullExpression.getLeftExpression(), expression, contextToTableAlias(context));
        return new WhereParseResult(expression, List.of(variableDescription));
    }

    @Override
    public <S> WhereParseResult visit(AndExpression andExpression, S context) {
        WhereParseResult leftResult = andExpression.getLeftExpression().accept(this, context);
        WhereParseResult rightResult = andExpression.getRightExpression().accept(this, context);
        return parseComplexExpression(leftResult, "&&", rightResult);
    }

    @Override
    public <S> WhereParseResult visit(OrExpression orExpression, S context) {
        WhereParseResult leftResult = orExpression.getLeftExpression().accept(this, context);
        WhereParseResult rightResult = orExpression.getRightExpression().accept(this, context);
        return parseComplexExpression(leftResult, "||", rightResult);
    }

    @Override
    public <S> WhereParseResult visit(Between between, S context) {
        TableAlias tableAlias = contextToTableAlias(context);

        String startStringExpression = between.isNot() ? "<" : ">=";
        String endStringExpression = between.isNot() ? ">" : "<=";
        String complexStringExpression = between.isNot() ? "||" : "&&";

        WhereParseResult startResult = parseBinaryExpression(between.getLeftExpression(), startStringExpression, between.getBetweenExpressionStart(), tableAlias);
        WhereParseResult endResult = parseBinaryExpression(between.getLeftExpression(), endStringExpression, between.getBetweenExpressionEnd(), tableAlias);
        return parseComplexExpression(startResult, complexStringExpression, endResult);
    }

    @Override
    public <S> WhereParseResult visit(ExpressionList<? extends net.sf.jsqlparser.expression.Expression> expressionList, S context) {
        List<WhereParseResult> whereParseResults = expressionList.stream()
                .map(e -> e.accept(this, context))
                .toList();
        return whereParseResults.getFirst();
    }

    private WhereParseResult parseComplexExpression(
            WhereParseResult leftResult,
            String stringExpression,
            WhereParseResult rightResult
    ) {
        Expression leftExpression = leftResult.expression();
        Expression rightExpression = rightResult.expression();
        String jepExpression = String.format(
                "(%s %s %s)",
                leftExpression.getExpressionString(),
                stringExpression,
                rightExpression.getExpressionString()
        );
        log.debug("Complex jepExpression {}", jepExpression);
        Expression expression = new Expression(jepExpression);
        List<VariableDescription> variableDescriptions = Stream.concat(
                leftResult.variableDescriptions().stream(),
                rightResult.variableDescriptions().stream()
        ).toList();
        variableDescriptions.forEach(v -> expression.setVariableDescription(v.name(), v.attributeRef()));
        return new WhereParseResult(expression, variableDescriptions);
    }

    private WhereParseResult parseBinaryExpression(
            net.sf.jsqlparser.expression.Expression leftExpression,
            String stringExpression,
            net.sf.jsqlparser.expression.Expression rightExpression,
            TableAlias tableAlias
    ) {
        String sqlExpressionWithoutAliases = String.format(
                "%s %s %s",
                expressionToValue(leftExpression),
                stringExpression,
                expressionToValue(rightExpression)
        );
        String jepExpression = sqlToJEPExpressionString(sqlExpressionWithoutAliases);
        log.debug("Binary jepExpression {}", jepExpression);
        Expression expression = new Expression(jepExpression);

        VariableDescription leftVariableDescription = setVariableDescription(leftExpression, expression, tableAlias);
        VariableDescription rightVariableDescription = setVariableDescription(rightExpression, expression, tableAlias);
        List<VariableDescription> variableDescriptions = Stream.of(leftVariableDescription, rightVariableDescription)
                .filter(Objects::nonNull)
                .toList();
        return new WhereParseResult(expression, variableDescriptions);
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

    private VariableDescription setVariableDescription(net.sf.jsqlparser.expression.Expression expression, Expression speedyExpression, TableAlias tableAlias) {
        if (!(expression instanceof Column column)) {
            return null;
        }
        String name = column.getColumnName();
        AttributeRef attributeRef = new AttributeRef(tableAlias, name);
        speedyExpression.setVariableDescription(name, attributeRef);
        return new VariableDescription(name, attributeRef);
    }

    private <S> TableAlias contextToTableAlias(S context) {
        if (!(context instanceof TableAlias)) {
            throw new IllegalArgumentException("Cannot parse where item without table alias!");
        }
        return (TableAlias) context;
    }

    public record WhereParseResult(Expression expression, List<VariableDescription> variableDescriptions) {
    }

    public record VariableDescription(String name, AttributeRef attributeRef) {
    }
}
