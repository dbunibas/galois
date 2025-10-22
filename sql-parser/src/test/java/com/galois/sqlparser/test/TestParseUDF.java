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
public class TestParseUDF {
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

        assertInstanceOf(Intersection.class, root);
        Intersection intersection = (Intersection) root;
        assertEquals(2, intersection.getChildren().size());

        assertInstanceOf(Select.class, intersection.getChildren().getFirst());
        Select select = (Select) intersection.getChildren().getFirst();
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());

        assertInstanceOf(UserDefinedFunction.class, intersection.getChildren().get(1));
        UserDefinedFunction udf = (UserDefinedFunction) intersection.getChildren().get(1);
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());
    }

    @Test
    public void testParseConditionAndUserDefinedFilterReverseOrder() {
        String sql = String.format("select * from %s e where udfilter('Is $1 a real name?', e.name) and e.salary >= 0", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        assertNotNull(root);

        assertInstanceOf(Intersection.class, root);
        Intersection intersection = (Intersection) root;
        assertEquals(2, intersection.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, intersection.getChildren().getFirst());
        UserDefinedFunction udf = (UserDefinedFunction) intersection.getChildren().getFirst();
        assertEquals(1, udf.getChildren().size());
        assertInstanceOf(Scan.class, udf.getChildren().getFirst());

        assertInstanceOf(Select.class, intersection.getChildren().get(1));
        Select select = (Select) intersection.getChildren().get(1);
        assertEquals(1, select.getChildren().size());
        assertInstanceOf(Scan.class, select.getChildren().getFirst());


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

        assertInstanceOf(Intersection.class, root);
        Intersection intersection = (Intersection) root;
        assertEquals(2, intersection.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, intersection.getChildren().getFirst());
        UserDefinedFunction udfLeft = (UserDefinedFunction) intersection.getChildren().getFirst();
        assertEquals(1, udfLeft.getChildren().size());
        assertInstanceOf(Scan.class, udfLeft.getChildren().getFirst());

        assertInstanceOf(UserDefinedFunction.class, intersection.getChildren().get(1));
        UserDefinedFunction udfRight = (UserDefinedFunction) intersection.getChildren().get(1);
        assertEquals(1, udfRight.getChildren().size());
        assertInstanceOf(Scan.class, udfRight.getChildren().getFirst());
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
    public void testParseComplexExpression00() {
        String sql = String.format("select * from %s e where (e.salary >= 0 or udfilter('0: Is $1 a real name?', e.name)) and udfilter('1: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);

        assertInstanceOf(Intersection.class, root);
        Intersection intersection = (Intersection) root;
        assertEquals(2, intersection.getChildren().size());

        Union union = (Union) intersection.getChildren().getFirst();
        assertEquals(2, union.getChildren().size());
        assertInstanceOf(Select.class, union.getChildren().getFirst());
        assertInstanceOf(UserDefinedFunction.class, union.getChildren().getLast());

        assertInstanceOf(UserDefinedFunction.class, intersection.getChildren().getLast());
        UserDefinedFunction last = (UserDefinedFunction) intersection.getChildren().getLast();
        assertEquals(1, last.getChildren().size());
        assertInstanceOf(Scan.class, last.getChildren().getFirst());
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
        // Query is complex: node name enumeration is given bottom-up
        String sql = String.format("select * from %s e where ((e.salary >= 0 or udfilter('0: Is $1 a real name?', e.name)) and udfilter('1: Is $1 a real name?', e.name)) or e.salary >= 1 or udfilter('2: Is $1 a real name?', e.name)", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql, UDF_FACTORY);
        log.info("{}", root);
        assertNotNull(root);
        assertInstanceOf(Union.class, root);
        assertEquals(2, root.getChildren().size());

        assertInstanceOf(Union.class, root.getChildren().getFirst());
        Union union01 = (Union) root.getChildren().getFirst();
        assertEquals(2, union01.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, root.getChildren().getLast());
        UserDefinedFunction udf02 = (UserDefinedFunction) root.getChildren().getLast();
        assertEquals(1, udf02.getChildren().size());
        assertInstanceOf(Scan.class, udf02.getChildren().getFirst());

        assertInstanceOf(Intersection.class, union01.getChildren().getFirst());
        Intersection intersection00 = (Intersection) union01.getChildren().getFirst();
        assertEquals(2, intersection00.getChildren().size());

        assertInstanceOf(Select.class, union01.getChildren().getLast());
        Select select01 = (Select) union01.getChildren().getLast();
        assertEquals(1, select01.getChildren().size());
        assertInstanceOf(Scan.class, select01.getChildren().getFirst());

        assertInstanceOf(Union.class, intersection00.getChildren().getFirst());
        Union union00 = (Union) intersection00.getChildren().getFirst();
        assertEquals(2, union00.getChildren().size());

        assertInstanceOf(UserDefinedFunction.class, intersection00.getChildren().getLast());
        UserDefinedFunction udf01 = (UserDefinedFunction) intersection00.getChildren().getLast();
        assertEquals(1, udf01.getChildren().size());
        assertInstanceOf(Scan.class, udf01.getChildren().getFirst());

        assertInstanceOf(Select.class, union00.getChildren().getFirst());
        Select select00 = (Select) union00.getChildren().getFirst();
        assertEquals(1, select00.getChildren().size());
        assertInstanceOf(Scan.class, select00.getChildren().getFirst());

        assertInstanceOf(UserDefinedFunction.class, union00.getChildren().getLast());
        UserDefinedFunction udf00 = (UserDefinedFunction) union00.getChildren().getLast();
        assertEquals(1, udf00.getChildren().size());
        assertInstanceOf(Scan.class, udf00.getChildren().getFirst());
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
