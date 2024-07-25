package com.galois.sqlparser.test;

import com.galois.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.*;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.ITable;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static com.galois.sqlparser.test.TestUtils.toTupleList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseGroupBy {
    private static final String TABLE_NAME = "EmpTable";

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testSimpleGroupBy() {
        String sql = String.format("select * from %s group by dept", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(GroupBy.class, root);
        GroupBy groupBy = (GroupBy) root;
        assertEquals(1, groupBy.getGroupingAttributes().size());
        assertEquals(1, groupBy.getAggregateFunctions().size());

        assertInstanceOf(Scan.class, groupBy.getChildren().getFirst());
        Scan scan = (Scan) groupBy.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals((int) (table.getSize() - 3), tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testGroupByWithAlias() {
        String sql = String.format("select t.dept as department from %s t group by t.dept", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAttributes(null, null).size());

        assertInstanceOf(GroupBy.class, project.getChildren().getFirst());
        GroupBy groupBy = (GroupBy) project.getChildren().getFirst();
        assertEquals(1, groupBy.getGroupingAttributes().size());
        assertEquals(1, groupBy.getAggregateFunctions().size());

        assertInstanceOf(Scan.class, groupBy.getChildren().getFirst());
        Scan scan = (Scan) groupBy.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals((int) (table.getSize() - 3), tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testGroupBy() {
        String sql = String.format("select t.dept, count(t.dept) as numDepts from %s t group by t.dept", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(2, project.getAttributes(null, null).size());
        assertFalse(project.isAggregative());
        assertEquals(1, project.getChildren().size());

        assertInstanceOf(GroupBy.class, project.getChildren().getFirst());
        GroupBy groupBy = (GroupBy) project.getChildren().getFirst();
        assertEquals(1, groupBy.getGroupingAttributes().size());
        assertEquals(2, groupBy.getAggregateFunctions().size());

        assertInstanceOf(Scan.class, groupBy.getChildren().getFirst());
        Scan scan = (Scan) groupBy.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals((int) (table.getSize() - 3), tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testGroupByOrderByLimit() {
        int limitSize = 10;
        String sql = String.format("select t.dept, count(t.dept) as numDepts from %s t group by t.dept order by t.numDepts desc limit %s", TABLE_NAME, limitSize);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Limit.class, root);
        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(limitSize, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testGroupByOrderByLimitWithWhere() {
        int limitSize = 2;
        String sql = String.format("select t.dept, count(t.dept) as numDepts from %s t where t.salary > 5000 group by t.dept order by numDepts desc limit %s", TABLE_NAME, limitSize);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Limit.class, root);
        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(limitSize, tuples.size());

        logTuples(tuples);
    }

    @Test
    @Disabled
    // FIXME: add support for aggregate functions when parsing "order by"
    public void testGroupByOrderWithAggregateFunction() {
        int limitSize = 1;
        String sql = String.format("select t.dept from %s t group by t.dept order by count(*) desc limit %s", TABLE_NAME, limitSize);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(limitSize, tuples.size());
        logTuples(tuples);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
