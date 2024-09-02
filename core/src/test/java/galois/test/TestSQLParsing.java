package galois.test;

import galois.parser.ParserFrom;
import galois.parser.ParserWhere;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestSQLParsing {

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
    
    @Test
    public void testExpressionANDSameVariable() {
        String expression = "a.elevation >=-50 AND a.elevation <=50";
        ParserWhere parser = new ParserWhere();
        parser.parseExpression(expression, true);
        Assertions.assertTrue(parser.getOperation().equals("AND"));
        System.out.println(parser.getExpressions().size());
        Assertions.assertTrue(parser.getExpressions().size() == 2);
    }

    @Test
    public void testFrom1Table() {
        String sql = "select * from target.actor a where a.name = 'Mario' AND a.age > 20 AND a.surname='Rossi'";
        ParserFrom parser = new ParserFrom();
        parser.parseFrom(sql);
        log.info("Parsed tables: {}", parser.getTables());
        Assertions.assertTrue(parser.getTables().size() == 1);
        Assertions.assertTrue(parser.getTables().get(0).equalsIgnoreCase("actor"));
    }

    @Test
    public void testFrom1TableWithoutAlias() {
        String sql = "select * from target.actor where name = 'Mario' AND age > 20 AND surname='Rossi'";
        ParserFrom parser = new ParserFrom();
        parser.parseFrom(sql);
        log.info("Parsed tables: {}", parser.getTables());
        Assertions.assertTrue(parser.getTables().size() == 1);
        Assertions.assertTrue(parser.getTables().get(0).equalsIgnoreCase("actor"));
    }
    
    @Test
    public void testFrom2Table() {
        String sql = "select * from target.actor a, target.movie m where a.id = m.id";
        log.info(sql);
        ParserFrom parser = new ParserFrom();
        parser.parseFrom(sql);
        log.info("Parsed tables: {}", parser.getTables());
        Assertions.assertTrue(parser.getTables().size() == 2);
        Assertions.assertTrue(parser.getTables().get(0).equalsIgnoreCase("actor"));
        Assertions.assertTrue(parser.getTables().get(1).equalsIgnoreCase("movie"));
    }
    
    @Test
    public void testFrom3Table() {
        String sql = "select * from target.actor a, target.movie m, target.director d where a.id = m.id and d.id = m.id";
        log.info(sql);
        ParserFrom parser = new ParserFrom();
        parser.parseFrom(sql);
        log.info("Parsed tables: {}", parser.getTables());
        Assertions.assertTrue(parser.getTables().size() == 3);
        Assertions.assertTrue(parser.getTables().get(0).equalsIgnoreCase("actor"));
        Assertions.assertTrue(parser.getTables().get(1).equalsIgnoreCase("movie"));
        Assertions.assertTrue(parser.getTables().get(2).equalsIgnoreCase("director"));
    }
    
    @Test
    public void testFrom3TableWithJoin() {
        String sql = "select * from target.actor a join target.movie m on a.id = m.id join target.director on d.id = a.id";
        log.info(sql);
        ParserFrom parser = new ParserFrom();
        parser.parseFrom(sql);
        log.info("Parsed tables: {}", parser.getTables());
        Assertions.assertTrue(parser.getTables().size() == 3);
        Assertions.assertTrue(parser.getTables().get(0).equalsIgnoreCase("actor"));
        Assertions.assertTrue(parser.getTables().get(1).equalsIgnoreCase("movie"));
        Assertions.assertTrue(parser.getTables().get(2).equalsIgnoreCase("director"));
    }
}
