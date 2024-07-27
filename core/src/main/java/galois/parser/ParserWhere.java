package galois.parser;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserWhere {

    private static final Logger logger = LoggerFactory.getLogger(ParserWhere.class);

    private ExpressionVisitorAdapter expressionVisitorAdapter;
    private SelectVisitorAdapter selectVisitorAdapter;
    private StatementVisitorAdapter statementVisitor;
    private boolean stopAtFirst = false;
    private String operation = "";
    private List<Expression> expressions = new ArrayList<>();

    public ParserWhere() {
        initParsers();
    }

    public String getOperation() {
        return operation;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public void parseWhere(String sql) throws ParserException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(statementVisitor);
        } catch (Exception e) {
            throw new ParserException("Error with parsing: " + sql + "\nMessage: " + e.getMessage());
        }
    }

    public void parseExpression(String expression, boolean fromXml) throws ParserException {
        try {
            String expString = expression;
            if (fromXml) {
                expString = StringEscapeUtils.unescapeXml(expression);
            }
            if (expString.contains(" AND ") && expString.contains((" OR "))) {
                stopAtFirst = true;
            }
            logger.debug("ExpString: " + expString);
            logger.debug("StopAtFirst: " + stopAtFirst);
            Expression exp = CCJSqlParserUtil.parseExpression(expString);
            logger.debug("Expression: " + exp);
            logger.debug("Expression type: " + exp.getClass().getName());
            exp.accept(expressionVisitorAdapter);
        } catch (Exception e) {
            throw new ParserException("Error with parsing: " + expression + "\nMessage: " + e.getMessage());
        }
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
                if (stopAtFirst) {
                    logger.debug("Left Expr: " + leftExpression);
                    logger.debug("Right Expr: " + rightExpression);
                    expressions.add(leftExpression);
                    expressions.add(rightExpression);
//                    return super.visitBinaryExpression(expr, context);
                    return null;
                }
                if (isAtom(leftExpression) && isAtom(rightExpression)) {
                    logger.debug("Create Leaf LLMScan on expr: " + expr);
                    logger.debug("Add LLMScan on the current");
                    expressions.add(expr);
                    return null;
                } else {
                    return super.visitBinaryExpression(expr, context);
                }
            }
            
            
            @Override
            public <S> Object visit(OrExpression expr, S context) {
                logger.debug("OR");
                operation = "OR";
                return super.visit(expr, context);
            }

            @Override
            public <S> Object visit(AndExpression expr, S context) {
                logger.debug("AND");
                operation = "AND";
                return super.visit(expr, context);
            }

//            @Override
//            public void visit(Parenthesis parenthesis) {
//                logger.debug("(PARENTHESIS)");
//                super.visit(parenthesis);
//            }

            private boolean isAtom(Expression expression) {
                if ((expression instanceof Column)
                        || (expression instanceof DateValue)
                        || (expression instanceof DoubleValue)
                        || (expression instanceof HexValue)
                        || (expression instanceof LongValue)
                        || (expression instanceof NullValue)
                        || (expression instanceof StringValue)
                        || (expression instanceof TimeValue)
                        || (expression instanceof TimeValue)
                        || (expression instanceof TimestampValue)) {
                    return true;
                }
                return false;
            }

        };

        this.selectVisitorAdapter = new SelectVisitorAdapter() {
            @Override
            public Object visit(PlainSelect plainSelect, Object context) {
                if (plainSelect.getWhere() != null) {
                    logger.debug("Expression in SelectVisitorAdapter: " + plainSelect.getWhere());
                    Expression where = plainSelect.getWhere();
                    try {
                        Expression expression = CCJSqlParserUtil.parseExpression(where.toString());
                        return expression.accept(expressionVisitorAdapter, null);
                        //return plainSelect.getWhere().accept(expressionVisitorAdapter, null);
                    } catch (Exception e) {

                    }
                }
                return super.visit(plainSelect, context); 
            }        
        };

        statementVisitor = new StatementVisitorAdapter() {
            @Override
            public Object visit(Select select, Object context) {
                return select.getPlainSelect().accept(selectVisitorAdapter, null);
                //return super.visit(select, context);
            }
        };
    }

}
