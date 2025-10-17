package com.bsf.sqlparser.test;

import com.bsf.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class TestParseWhere {
    private static final String TABLE_NAME = "EmpTable";

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testEqualsToNoAlias() {
        String sql = String.format("select * from %s where name = 'TMDXWSCUHSE'", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(table.getAttributes().size() + 1, tuple.getCells().size());

        logTuples(tuples);
    }

    @Test
    public void testEqualsToWithAlias() {
        String sql = String.format("select * from %s t where 1010 = t.salary", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(table.getAttributes().size() + 1, tuple.getCells().size());

        logTuples(tuples);
    }

    @Test
    public void testNotEqualsTo() {
        String sql = String.format("select * from %s t where t.salary <> 1010", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize() - 1, tuples.size());
    }

    @Test
    public void testGreaterThan() {
        String sql = String.format("select * from %s t where t.salary > 3500", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(18, tuples.size());

        logTuples(tuples.subList(0, 5));
    }

    @Test
    public void testLessThan() {
        String sql = String.format("select * from %s t where t.salary <= 1010", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(table.getAttributes().size() + 1, tuple.getCells().size());

        logTuples(tuples);
    }

    @Test
    public void testIsNotNull() {
        String sql = String.format("select * from %s t where t.manager is not null", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize() - 2, tuples.size());

        logTuples(tuples.subList(0, 5));
    }

    @Test
    @Disabled
    // FIXME: isNull seems unsupported by JEP
    public void testIsNull() {
        String sql = String.format("select * from %s t where t.manager is null", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());
        assertEquals(1, select.getChildren().size());

        assertInstanceOf(Scan.class, select.getChildren().getFirst());
        Scan scan = (Scan) select.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(2, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testBetween() {
        String sql = String.format("select * from %s t where t.salary between 9998 and 100001", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(3, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testNotBetween() {
        String sql = String.format("select * from %s t where t.salary not between 9998 and 100001", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize() - 3, tuples.size());
    }

    @Test
    public void testNegativeValue() {
        String sql = String.format("select * from %s t where t.salary > -50000 and t.salary < 1000000", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
