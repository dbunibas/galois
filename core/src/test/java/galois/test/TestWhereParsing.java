package galois.test;

import galois.parser.ParserWhere;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestWhereParsing {

    @Test
    public void testSimpleSelect() {
//        String sql = "select * from target.actor a where (a.name='Mario' and a.age > 20) or (a.nationality='Italian' and a.income < 100) and not(a.surname = 'Rossi')";
        String sql = "select * from target.actor a where a.name = 'Mario' AND a.age > 20 AND a.surname='Rossi'";
        ParserWhere parser = new ParserWhere();
        parser.parseWhere(sql);
        log.info("Parsed expressions: {}", parser.getExpressions());
        log.info("Parser operation: {}", parser.getOperation());
    }
    
    @Test
    public void testIsNotNull() {
        String sql = "select m.startyear, count(*) as numMovies from target.movie m where m.director = 'Steven Spielberg' AND m.startyear is not null group by m.startyear";
        ParserWhere parser = new ParserWhere();
        parser.parseWhere(sql);
        log.info("Parsed expressions: {}", parser.getExpressions());
        log.info("Parser operation: {}", parser.getOperation());
    }

    @Test
    public void testExpressionComplex() {
        String expression = "(((a.name = 'Mario') AND (a.age &gt; 20)) OR ((a.nationality = 'Italian') AND (a.income &lt; 100) AND (a.surname &lt;&gt; 'Rossi')))";
        ParserWhere parser = new ParserWhere();
        parser.parseExpression(expression, true);
        List<Expression> expressions = parser.getExpressions();
        System.out.println(expressions);
        Assertions.assertTrue(parser.getOperation().equals("OR"));
        Assertions.assertTrue(parser.getExpressions().size() == 2);
    }

    @Test
    public void testExpressionAND() {
        String expression = "a.name = 'Mario' AND a.age > 20 AND a.surname='Rossi'";
        ParserWhere parser = new ParserWhere();
        parser.parseExpression(expression, true);
        Assertions.assertTrue(parser.getOperation().equals("AND"));
        Assertions.assertTrue(parser.getExpressions().size() == 3);
    }

    @Test
    public void testExpressionOR() {
        String expression = "a.name = 'Mario' OR a.age > 20 OR a.surname='Rossi' OR a.income < 1000";
        ParserWhere parser = new ParserWhere();
        parser.parseExpression(expression, true);
        Assertions.assertTrue(parser.getOperation().equals("OR"));
        Assertions.assertTrue(parser.getExpressions().size() == 4);
    }
}
