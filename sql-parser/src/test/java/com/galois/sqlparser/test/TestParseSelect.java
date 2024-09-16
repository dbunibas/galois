package com.galois.sqlparser.test;

import com.galois.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.Distinct;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Project;
import speedy.model.algebra.Scan;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.ITable;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static com.galois.sqlparser.test.TestUtils.toTupleList;
import static com.galois.sqlparser.test.TestUtils.toTupleStream;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseSelect {
    private static final String TABLE_NAME = "EmpTable";

    private MainMemoryDB db;

    @BeforeEach
    public void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testSelectStar() {
        String sql = String.format("select * from %s", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        log.info("root is {}", root);
        assertNotNull(root);

        assertInstanceOf(Scan.class, root);
        Scan scan = (Scan) root;
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());
        tuples.forEach(t -> assertEquals(table.getAttributes().size() + 1, t.getCells().size()));

        logTuples(tuples);
    }

    @Test
    public void testSelectAliasStar() {
        String sql = String.format("select t.* from %s t", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(Scan.class, root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());
        tuples.forEach(t -> assertEquals(table.getAttributes().size() + 1, t.getCells().size()));
    }

    @Test
    public void testSelectSingleAttributeNoAlias() {
        String sql = String.format("select name from %s", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAttributes(null, null).size());
        assertFalse(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        Scan scan = (Scan) project.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());
        tuples.forEach(t -> assertEquals(1 + 1, t.getCells().size()));

        // logTuples(tuples.subList(0, 5));
    }

    @Test
    public void testSelectMultipleAttributesWithAlias() {
        String sql = String.format("select t.name, t.salary from %s t", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(2, project.getAttributes(null, null).size());
        assertFalse(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        Scan scan = (Scan) project.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(table.getSize(), tuples.size());
        tuples.forEach(t -> assertEquals(2 + 1, t.getCells().size()));

        // logTuples(tuples.subList(0, 5));
    }

    @Test
    public void testSelectCountStar() {
        String sql = String.format("select count(*) from %s", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        Scan scan = (Scan) project.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(1, tuple.getCells().size());
        assertEquals("count", tuple.getCells().getFirst().getAttribute());
        assertEquals((int) table.getSize(), tuple.getCells().getFirst().getValue().getPrimitiveValue());

//        logTuples(tuples);
    }

    @Test
    public void testSelectCountAttribute() {
        String sql = String.format("select count(name) from %s", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        Scan scan = (Scan) project.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(1, tuple.getCells().size());
        assertEquals("name", tuple.getCells().getFirst().getAttribute());
        assertEquals((int) table.getSize(), tuple.getCells().getFirst().getValue().getPrimitiveValue());

//        logTuples(tuples);
    }

    @Test
    public void testSelectMin() {
        String sql = String.format("select min(t.salary) from %s t", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Project.class, root);
        Project project = (Project) root;
        assertEquals(1, project.getAggregateFunctions().size());
        assertTrue(project.isAggregative());
        assertEquals(1, project.getChildren().size());
        assertInstanceOf(Scan.class, project.getChildren().getFirst());

        Scan scan = (Scan) project.getChildren().getFirst();
        assertEquals(TABLE_NAME, scan.getTableAlias().getTableName());
        assertEquals("t", scan.getTableAlias().getAlias());
        assertEquals(0, scan.getChildren().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(1, tuple.getCells().size());
        assertEquals("salary", tuple.getCells().getFirst().getAttribute());
        assertEquals(1010.0, tuple.getCells().getFirst().getValue().getPrimitiveValue());

        logTuples(tuples);
    }

    @Test
    public void testSelectMax() {
        String sql = String.format("select max(t.salary) from %s t", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        Tuple tuple = tuples.getFirst();
        assertEquals(1, tuple.getCells().size());
        assertEquals("salary", tuple.getCells().getFirst().getAttribute());
        assertEquals(100000.0, tuple.getCells().getFirst().getValue().getPrimitiveValue());

        logTuples(tuples);
    }

    @Test
    public void testSelectUsingSubtraction() {
        String sql = String.format("select (t.salary - t.salary) as difference from %s t", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);

        tuples.forEach(t -> assertEquals(0.0, t.getCells().getLast().getValue().getPrimitiveValue()));
    }

    @Test
    public void testSelectUsingSubtractionWithOrderBy() {
        String sql = String.format("select (t.salary - t.salary) as difference from %s t order by difference", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);

        tuples.forEach(t -> assertEquals(0.0, t.getCells().getLast().getValue().getPrimitiveValue()));
    }

    @Test
    public void testDistinct() {
        String sql = String.format("select distinct(t.name) from %s t", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Distinct.class, root);
        assertEquals(1, root.getChildren().size());
        assertInstanceOf(Project.class, root.getChildren().getFirst());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);

        ITable table = db.getTable(TABLE_NAME);
        List<String> distinctCells = toTupleStream(table.getTupleIterator())
                .map(t -> t.getCell(new AttributeRef(TABLE_NAME, "name")).getValue().toString())
                .distinct()
                .toList();
        assertEquals(distinctCells.size(), tuples.size());
    }

    @Test
    public void testProjectionWithAlias() {
        String sql = String.format("select t.manager, count(t.manager) man from %s t group by t.manager order by man desc limit 1", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);
        log.info("root is {}", root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        logTuples(tuples);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
