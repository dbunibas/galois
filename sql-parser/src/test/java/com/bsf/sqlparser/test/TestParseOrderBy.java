package com.bsf.sqlparser.test;

import com.bsf.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Limit;
import queryexecutor.model.algebra.OrderBy;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseOrderBy {
    private static final String TABLE_NAME = "EmpTable";

    private static MainMemoryDB db;

    @BeforeAll
    public static void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testOrderByAsc() {
        String sql = String.format("select * from %s order by salary", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(OrderBy.class, root);
        OrderBy orderBy = (OrderBy) root;
        assertEquals(OrderBy.ORDER_ASC, orderBy.getOrder());
        assertEquals(1, orderBy.getAttributes(null, null).size());
        assertEquals("salary", orderBy.getAttributes(null, null).getFirst().getName());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testOrderByDesc() {
        String sql = String.format("select * from %s order by salary desc", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(OrderBy.class, root);
        OrderBy orderBy = (OrderBy) root;
        assertEquals(OrderBy.ORDER_DESC, orderBy.getOrder());
        assertEquals(1, orderBy.getAttributes(null, null).size());
        assertEquals("salary", orderBy.getAttributes(null, null).getFirst().getName());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testOrderByDescWithLimit() {
        int limitSize = 15;
        String sql = String.format("select * from %s order by salary desc limit %d", TABLE_NAME, limitSize);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Limit.class, root);
        Limit limit = (Limit) root;
        assertEquals(limitSize, limit.getSize());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(limitSize, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testOrderByWithProject() {
        int limitSize = 1;
        String sql = String.format("SELECT t.name from %s t where t.salary > 5000 order by t.salary desc limit %d", TABLE_NAME, limitSize);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Limit.class, root);
        Limit limit = (Limit) root;
        assertEquals(limitSize, limit.getSize());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(limitSize, tuples.size());

        Tuple tuple = tuples.getFirst();
        assertEquals("oid", tuple.getCells().getFirst().getAttribute());
        assertEquals("RafaelNadal", tuple.getCells().getLast().getValue().getPrimitiveValue());
    }

    @Test
    public void testOrderByUsingAlias() {
        String sql = String.format("SELECT t.name as empName from %s t order by empName", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);

        logTuples(tuples);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
