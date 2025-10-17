package bsf.parser;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;

public class ParserProvenance {

    private static final Logger logger = LoggerFactory.getLogger(ParserProvenance.class);

    private ExpressionVisitorAdapter expressionVisitorAdapter;
    private SelectVisitorAdapter selectVisitorAdapter;
    private StatementVisitorAdapter statementVisitor;
    private Set<String> attributeProvenance = new HashSet<>();
    private Set<String> tablesProvenance = new HashSet<>();
    private Set<String> attributesInSelect = new HashSet<>();
    private Set<String> attributeInAggregate = new HashSet<>();
    private IDatabase database;

    public ParserProvenance(IDatabase database) {
        initParsers();
        this.database = database;
    }

    public void parse(String sql) throws ParserException {
        try {
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(sql);
            this.tablesProvenance = new HashSet(parserFrom.getTables());
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(statementVisitor);
        } catch (Exception e) {
            throw new ParserException("Error with parsing: " + sql + "\nMessage: " + e.getMessage());
        }
    }

    public Set<String> getAttributeProvenance() {
        return attributeProvenance;
    }

    public Set<String> getTablesProvenance() {
        return tablesProvenance;
    }

    public Set<String> getAttributesInSelect() {
        return attributesInSelect;
    }
    
    private boolean isColumn(Expression expression) {
        return expression instanceof Column;
    }

    private String cleanAttribute(String attributeName) {
        if (attributeName.contains(".")) {
            return attributeName.substring(attributeName.indexOf(".") + 1);
        }
        return attributeName;
    }
    
    private boolean checkAttributeName(String attributeName) {
        if (this.database == null) return false;
        try {
            for (String tableName : tablesProvenance) {
                ITable table = this.database.getTable(tableName);
                if (table.getAttribute(attributeName) != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    
    private Set<String> getAllAttributes() {
        if (this.database == null) return new HashSet<>();
        Set<String> attributesInTables = new HashSet<>();
        for (String tableName : tablesProvenance) {
            ITable table = this.database.getTable(tableName);
            for (Attribute attribute : table.getAttributes()) {
                attributesInTables.add(attribute.getName());
            }
        }
        return attributesInTables;
    }

    private void initParsers() {
        this.expressionVisitorAdapter = new ExpressionVisitorAdapter<>() {

            @Override
            public <S> Object visit(Select selectBody, S context) {
                logger.debug("Visit select: " + selectBody);
                return super.visit(selectBody, context);
            }

            @Override
            public <S> Object visit(ExpressionList<?> expressionList, S context) {
                logger.debug("Visit ExpressionList: " + expressionList);
                String attribute = cleanAttribute(expressionList.toString());
                if (attribute.equals("*")) {
                    attributeProvenance.addAll(getAllAttributes());
                } else {
                   attributeProvenance.add(attribute);
                }
                return super.visit(expressionList, context);
            }

            @Override
            public <S> Object visit(ExtractExpression expr, S context) {
                logger.debug("Visit ExtractExpression: " + expr);
                return super.visit(expr, context);
            }

            @Override
            protected <S> Object visitBinaryExpression(BinaryExpression expr, S context) {
                logger.debug("Visit Binary: " + expr);
                Expression leftExpression = expr.getLeftExpression();
                Expression rightExpression = expr.getRightExpression();
                if (isColumn(leftExpression)) {
                    String attrName = leftExpression.toString();
                    logger.debug("Attribute: " + attrName);
                    attributeProvenance.add(cleanAttribute(attrName));
                } else if (isColumn(rightExpression)) {
                    String attrName = rightExpression.toString();
                    logger.debug("Attribute: " + attrName);
                    attributeProvenance.add(cleanAttribute(attrName));
                } else {
                    logger.debug("Visit Left Expr: " + leftExpression);
                    leftExpression.accept(this, context);
                    logger.debug("Visit Right Expr: " + rightExpression);
                    rightExpression.accept(this, context);
                }
                return null;
            }

        };

        this.selectVisitorAdapter = new SelectVisitorAdapter() {
            @Override
            public Object visit(PlainSelect plainSelect, Object context) {
                List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
                logger.debug("Select Items: {}", selectItems);
                for (SelectItem<?> selectItem : selectItems) {
                    if (selectItem.getExpression() != null) {
                        logger.debug("expression in select: {}", selectItem.getExpression());
                        if (isColumn(selectItem.getExpression())) {
                            attributeProvenance.add(cleanAttribute(selectItem.getExpression().toString()));
                            attributesInSelect.add(cleanAttribute(selectItem.getExpression().toString()));
                        } else {
                            selectItem.getExpression().accept(expressionVisitorAdapter, null);
                        }
                    } else {
                        logger.info("Single attribute: {}", selectItem.toString());
                        attributeProvenance.add(cleanAttribute(selectItem.toString()));
                        attributesInSelect.add(cleanAttribute(selectItem.toString()));
                    }
                }
                if (plainSelect.getWhere() != null) {
                    logger.debug("Expression in SelectVisitorAdapter: " + plainSelect.getWhere());
                    Expression where = plainSelect.getWhere();
                    try {
                        Expression expression = CCJSqlParserUtil.parseExpression(where.toString());
                        expression.accept(expressionVisitorAdapter, null);
                        //return plainSelect.getWhere().accept(expressionVisitorAdapter, null);
                    } catch (Exception e) {

                    }
                }
                if (plainSelect.getGroupBy() != null) {
                    logger.debug("GrouBy: {}", plainSelect.getGroupBy());
                    ExpressionList groupByExpressionList = plainSelect.getGroupBy().getGroupByExpressionList();
                    if (groupByExpressionList != null) {
                        groupByExpressionList.accept(expressionVisitorAdapter);
                    }
                }
                if (plainSelect.getHaving() != null) {
                    logger.debug("Having: {}", plainSelect.getHaving());
                    Expression expressionHaving = plainSelect.getHaving();
                    expressionHaving.accept(expressionVisitorAdapter);
                }
                if (plainSelect.getOrderByElements() != null) {
                    List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
                    logger.debug("Order by: {}", orderByElements);
                    for (OrderByElement orderByElement : orderByElements) {
                        Expression expression = orderByElement.getExpression();
                        logger.debug("Order by Expression: {}", expression);
                        String orderName = orderByElement.toString();
                        String[] split = orderName.split(" ");
                        String attrOrderByName = cleanAttribute(split[0]);
                        logger.debug("Order by to Check: {}", attrOrderByName);
                        if (database != null && checkAttributeName(attrOrderByName)) {
                            logger.debug("Order by add: {}", attrOrderByName);
                            attributeProvenance.add(attrOrderByName);
                        } 
                    }
                }
                if (plainSelect.getJoins() != null) {
                    List<Join> joins = plainSelect.getJoins();
                    logger.debug("Joins: {}", joins);
                    for (Join join : joins) {
                        Collection<Expression> onExpressions = join.getOnExpressions();
                        for (Expression onExpression : onExpressions) {
                            onExpression.accept(expressionVisitorAdapter);
                        }
                    }
                }
                return super.visit(plainSelect, context);
            }
        };

        statementVisitor = new StatementVisitorAdapter() {
            @Override
            public Object visit(Select select, Object context) {
                return select.getPlainSelect().accept(selectVisitorAdapter, null);
            }
        };
    }

}
