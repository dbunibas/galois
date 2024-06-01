package galois.parser;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
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
            Expression exp = CCJSqlParserUtil.parseExpression(expString);
            exp.accept(expressionVisitorAdapter);
        } catch (Exception e) {
            throw new ParserException("Error with parsing: " + expression + "\nMessage: " + e.getMessage());
        }
    }

    private void initParsers() {
        this.expressionVisitorAdapter = new ExpressionVisitorAdapter() {

            @Override
            protected void visitBinaryExpression(BinaryExpression expr) {
                Expression leftExpression = expr.getLeftExpression();
                Expression rightExpression = expr.getRightExpression();
                if (stopAtFirst) {
                    logger.debug("Left Expr: " + leftExpression);
                    logger.debug("Right Expr: " + rightExpression);
                    expressions.add(leftExpression);
                    expressions.add(rightExpression);
                    return;
                }
                if (isAtom(leftExpression) && isAtom(rightExpression)) {
                    logger.debug("Create Leaf LLMScan on expr: " + expr);
                    logger.debug("Add LLMScan on the current");
                    expressions.add(expr);
                } else {
                    super.visitBinaryExpression(expr);
                }

            }

//            @Override
//            public void visit(NotExpression notExpr) {
//                super.visit(notExpr);
//            }
            
            @Override
            public void visit(OrExpression expr) {
                logger.debug("OR");
                operation = "OR";
                super.visit(expr);
            }

            @Override
            public void visit(AndExpression expr) {
                logger.debug("AND");
                operation = "AND";
                super.visit(expr);
            }

//            @Override
//            public void visit(Parenthesis expr) {
//                logger.debug("(PARENTHESIS)");
//                super.visit(expr);
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

//            @Override
//            public void visit(Column column) {
//               logger.info("Found a Column " + column.getColumnName());
//            }

        };

        this.selectVisitorAdapter = new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                if (plainSelect.getWhere() != null) {
                    logger.debug("Expression: " + plainSelect.getWhere());
                    Expression where = plainSelect.getWhere();
                    try {
                        Expression expression = CCJSqlParserUtil.parseExpression(where.toString());
                        plainSelect.getWhere().accept(expressionVisitorAdapter);
                    } catch (Exception e) {

                    }
                }
            }

        };

        statementVisitor = new StatementVisitorAdapter() {

            @Override
            public void visit(Select select) {
                select.getPlainSelect().accept(selectVisitorAdapter);
            }
        };
    }

}
