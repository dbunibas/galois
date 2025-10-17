package com.bsf.sqlparser.test;

import com.bsf.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import queryexecutor.model.algebra.*;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static com.bsf.sqlparser.test.TestUtils.toTupleList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestParseJoin {
    private static final String TABLE_NAME = "EmpTable";

    private static MainMemoryDB db;

    @BeforeAll
    public static void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testSimpleJoin() {
        String sql = String.format("select * from %s t1 join %s t2 on t1.manager = t2.name", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);
        assertInstanceOf(Join.class, root);

        Join join = (Join) root;
        assertEquals(2, join.getChildren().size());
        assertInstanceOf(Scan.class, join.getChildren().getFirst());
        assertInstanceOf(Scan.class, join.getChildren().getLast());

        assertEquals(1, join.getLeftAttributes().size());
        assertEquals("manager", join.getLeftAttributes().getFirst().getName());

        assertEquals(1, join.getRightAttributes().size());
        assertEquals("name", join.getRightAttributes().getFirst().getName());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(2, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testJoinWithSimpleFilter() {
        String sql = String.format("select * from %s t1 join %s t2 on t1.manager = t2.name where t1.salary > 2000", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);
        assertInstanceOf(Join.class, root);

        Join join = (Join) root;
        assertEquals(2, join.getChildren().size());

        assertInstanceOf(Select.class, join.getChildren().getFirst());
        assertInstanceOf(Scan.class, join.getChildren().getLast());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
    }

    @Test
    public void testJoinWithFilter() {
        String sql = String.format("select * from %s t1 join %s t2 on t1.manager = t2.name where t1.salary > 2000 && t1.name = 'XUXOIWLXSVE2'", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);
        assertInstanceOf(Join.class, root);

        Join join = (Join) root;
        assertEquals(2, join.getChildren().size());
        assertInstanceOf(Select.class, join.getChildren().getFirst());
        assertInstanceOf(Scan.class, join.getChildren().getLast());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
    }

    @Test
    public void testJoinWithProject() {
        String sql = String.format("select t1.name, t1.salary, t2.name as manager_name from %s t1 join %s t2 on t1.manager = t2.name where t1.salary > 2000 && t1.name = 'XUXOIWLXSVE2'", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(Project.class, root);

        Project project = (Project) root;
        assertEquals(1, project.getChildren().size());
        assertEquals(3, project.getAttributes(null, null).size());
        assertInstanceOf(Join.class, project.getChildren().getFirst());

        Join join = (Join) project.getChildren().getFirst();
        assertEquals(2, join.getChildren().size());
        assertInstanceOf(Select.class, join.getChildren().getFirst());
        assertInstanceOf(Scan.class, join.getChildren().getLast());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testJoinWithMax() {
        String sql = String.format("select max(t1.salary) as max_salary from %s t1 join %s t2 on t1.manager = t2.name", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        log.info("root is {}", root);
        assertNotNull(root);
        assertInstanceOf(Project.class, root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());

        logTuples(tuples);
    }

    @Test
    public void testJoinWithOrderBy() {
        String sql = String.format("select t1.name, t1.salary, t2.name as manager_name, t2.salary as manager_salary from %s as t1 join %s as t2 on t1.manager = t2.name order by t1.salary desc limit 1", TABLE_NAME, TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);
        assertInstanceOf(Limit.class, root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = toTupleList(tupleIterator);
        assertEquals(1, tuples.size());

        logTuples(tuples);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
