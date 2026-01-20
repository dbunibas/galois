package com.galois.sqlparser.test;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.ParseContext;
import com.galois.sqlparser.SQLQueryParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.*;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.algebra.operators.ListTupleIterator;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.algebra.udf.UserDefinedFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseUDFFilter {
    private static final String TABLE_NAME = "EmpTable";
    private static final IUserDefinedFunctionFactory UDF_FACTORY = new MockUDFilterFactory();

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testParseUserDefinedFilter() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        UserDefinedFunction udf = (UserDefinedFunction) root;
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, udf.getChildren().getFirst());

        ITupleIterator iterator = root.execute(db, db);
        List<Tuple> tuples = TestUtils.toTupleList(iterator);
        assertEquals(1, tuples.size());
        log.info("{}", tuples.getFirst());
    }

    @Test
    public void testParseConditionAndUserDefinedFilter() {
        String sql = String.format("select * from %s e where e.salary >= 0 and udfilter('Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        assertEquals(1, root.getChildren().size());

        assertInstanceOf(Select.class, root.getChildren().getFirst());
        Select select = (Select) root.getChildren().getFirst();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        assertEquals(0, select.getChildren().getFirst().getChildren().size());
    }

    @Test
    public void testParseConditionAndUserDefinedFilterReverseOrder() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name) and e.salary >= 0", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        assertEquals(1, root.getChildren().size());

        assertInstanceOf(Select.class, root.getChildren().getFirst());
        Select select = (Select) root.getChildren().getFirst();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        assertEquals(0, select.getChildren().getFirst().getChildren().size());
    }

    @Test
    public void testParseConditionOrUserDefinedFilter() {
        String sql = String.format("select * from %s e where e.salary >= 100000 or udfilter('Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(Union.class, root);
        Union union = (Union) root;
        assertEquals(2, union.getChildren().size());
        assertInstanceOf(Select.class, union.getChildren().getFirst());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getLast());

        Select select = (Select) union.getChildren().getFirst();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());

        UserDefinedFunction udf = (UserDefinedFunction) union.getChildren().getLast();
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, udf.getChildren().getFirst());

        assertSame(select.getChildren().getFirst(), udf.getChildren().getFirst());
    }

    @Test
    public void testParseConditionOrUserDefinedFilterReverseOrder() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name) or e.salary >= 100000", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Union.class, root);
        Union union = (Union) root;
        assertEquals(2, union.getChildren().size());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getFirst());
        assertInstanceOf(Select.class, union.getChildren().getLast());

        UserDefinedFunction udf = (UserDefinedFunction) union.getChildren().getFirst();
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, udf.getChildren().getFirst());

        Select select = (Select) union.getChildren().getLast();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());

        assertSame(select.getChildren().getFirst(), udf.getChildren().getFirst());
    }

    @Test
    public void testParseUDFAndUDF() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name) and udfilter('Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        assertEquals(1, root.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, root.getChildren().getFirst());
        UserDefinedFunction udf = (UserDefinedFunction) root.getChildren().getFirst();
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, udf.getChildren().getFirst());
        assertEquals(0, udf.getChildren().getFirst().getChildren().size());
    }

    @Test
    public void testParseUDFOrUDF() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name) or udfilter('Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Union.class, root);
        Union union = (Union) root;
        assertEquals(2, union.getChildren().size());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getFirst());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getLast());

        UserDefinedFunction first = (UserDefinedFunction) union.getChildren().getFirst();
        assertEquals(1, first.getChildren().size());
        assertInstanceOf(Scan.class, first.getChildren().getFirst());

        UserDefinedFunction last = (UserDefinedFunction) union.getChildren().getLast();
        assertEquals(1, last.getChildren().size());
        assertInstanceOf(Scan.class, last.getChildren().getFirst());

        assertSame(first.getChildren().getFirst(), last.getChildren().getFirst());
    }

    @Test
    public void testThreeWayAnd() {
        String sql = String.format("select * from %s e where (e.salary >= 0 and udfilter('0: Is $1 a real name?', e.name)) and udfilter('1: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        assertEquals(1, root.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, root.getChildren().getFirst());
        assertEquals(1, root.getChildren().getFirst().getChildren().size());
        assertInstanceOf(Select.class, root.getChildren().getFirst().getChildren().getFirst());
        assertEquals(1, root.getChildren().getFirst().getChildren().getFirst().getChildren().size());
        assertInstanceOf(Scan.class, root.getChildren().getFirst().getChildren().getFirst().getChildren().getFirst());
    }

    @Test
    public void testParseComplexExpression00() {
        String sql = String.format("select * from %s e where (e.salary >= 0 or udfilter('0: Is $1 a real name?', e.name)) and udfilter('1: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(UserDefinedFunction.class, root);
        assertEquals(1, root.getChildren().size());

        assertInstanceOf(Union.class, root.getChildren().getFirst());
        Union union = (Union) root.getChildren().getFirst();
        assertEquals(2, union.getChildren().size());
        assertInstanceOf(Select.class, union.getChildren().getFirst());
        assertEquals(1, union.getChildren().getFirst().getChildren().size());
        assertInstanceOf(Scan.class, union.getChildren().getFirst().getChildren().getFirst());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getLast());
        assertEquals(1, union.getChildren().getLast().getChildren().size());
        assertInstanceOf(Scan.class, union.getChildren().getLast().getChildren().getFirst());
    }

    @Test
    public void testParseComplexExpression01() {
        String sql = String.format("select * from %s e where (e.salary >= 0 or udfilter('0: Is $1 a real name?', e.name)) or udfilter('1: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(Union.class, root);
        Union union = (Union) root;
        assertEquals(2, union.getChildren().size());

        Union subUnion = (Union) union.getChildren().getFirst();
        assertEquals(2, subUnion.getChildren().size());
        assertInstanceOf(Select.class, subUnion.getChildren().getFirst());
        assertInstanceOf(UserDefinedFunction.class, subUnion.getChildren().getLast());

        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getLast());
        UserDefinedFunction last = (UserDefinedFunction) union.getChildren().getLast();
        assertEquals(1, last.getChildren().size());
        assertInstanceOf(Scan.class, last.getChildren().getFirst());
    }

    @Test
    public void testParseComplexExpressionA() {
        String sql = String.format("select * from %s e where ((e.salary >= 0 or udfilter('0: Is $1 a real name?', e.name)) and udfilter('1: Is $1 a real name?', e.name)) or e.salary >= 1 or udfilter('2: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);

        // TODO: add assertions
    }

    @Test
    @Disabled
    public void testParseComplexExpressionB() {
        String sql = String.format("select * from %s e where (udfilter('0: Is $1 a real name?', e.name) or udfilter('0: Is $1 a real name?', e.name)) and (udfilter('0: Is $1 a real name?', e.name) or udfilter('0: Is $1 a real name?', e.name)) and (udfilter('0: Is $1 a real name?', e.name) or udfilter('0: Is $1 a real name?', e.name))", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        // TODO: add assertions
    }

    @RequiredArgsConstructor
    private static final class YannikSinnerUDFilter implements IUserDefinedFunction {
        private final AttributeRef attributeRef;

        @Override
        public ITupleIterator execute(ITupleIterator iterator) {
            ArrayList<Tuple> result = new ArrayList<>();
            while (iterator.hasNext()) {
                Tuple tuple = iterator.next();
                Cell cell = tuple.getCell(attributeRef);
                if (cell.getValue().getPrimitiveValue().equals("YannikSinner")) {
                    result.add(tuple);
                }
            }
            iterator.close();
            return new ListTupleIterator(result);
        }
    }

    private static final class MockUDFilterFactory implements IUserDefinedFunctionFactory {
        @Override
        public IUserDefinedFunction getUserDefinedFunction(String name, ExpressionList<? extends Expression> expressions, ParseContext parseContext) {
            if (!name.equalsIgnoreCase("udfilter")) throw new UnsupportedOperationException();

            Expression expression = expressions.get(1);
            if (expression instanceof Column column) {
                TableAlias tableAlias = parseContext.getTableAliasFromColumn(column);
                AttributeRef attributeRef = new AttributeRef(tableAlias, column.getColumnName());
                return new YannikSinnerUDFilter(attributeRef);
            }

            throw new UnsupportedOperationException();
        }
    }
}
