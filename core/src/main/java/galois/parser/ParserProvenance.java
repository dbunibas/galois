package galois.parser;

import java.util.ArrayList;
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
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserProvenance {

    private static final Logger logger = LoggerFactory.getLogger(ParserProvenance.class);

    private ExpressionVisitorAdapter expressionVisitorAdapter;
    private SelectVisitorAdapter selectVisitorAdapter;
    private StatementVisitorAdapter statementVisitor;
    private Set<String> attributeProvenance = new HashSet<>();
    private Set<String> tablesProvenance = new HashSet<>();

    public ParserProvenance() {
        initParsers();
    }

    public void parse(String sql) throws ParserException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(statementVisitor);
            ParserFrom parserFrom = new ParserFrom();
            parserFrom.parseFrom(sql);
            this.tablesProvenance = new HashSet(parserFrom.getTables());
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
                attributeProvenance.add(expressionList.toString());
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
                    attributeProvenance.add(attrName);
                } else if (isColumn(rightExpression)) {
                    String attrName = rightExpression.toString();
                    logger.debug("Attribute: " + attrName);
                    attributeProvenance.add(attrName);
                } else {
                    logger.debug("Visit Left Expr: " + leftExpression);
                    leftExpression.accept(this, context);
                    logger.debug("Visit Right Expr: " + rightExpression);
                    rightExpression.accept(this, context);
                }
                return null;
            }

            private boolean isColumn(Expression expression) {
                return expression instanceof Column;
            }

        };

        this.selectVisitorAdapter = new SelectVisitorAdapter() {
            @Override
            public Object visit(PlainSelect plainSelect, Object context) {
                List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
                logger.debug("Select Items: {}", selectItems);
                for (SelectItem<?> selectItem : selectItems) {
                    if (selectItem.getExpression() != null) {
                        selectItem.getExpression().accept(expressionVisitorAdapter, null);
                    } else {
                        attributeProvenance.add(selectItem.toString());
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
                        String orderName = orderByElement.toString();
                        String[] split = orderName.split(" ");
                        attributeProvenance.add(split[0]);
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
