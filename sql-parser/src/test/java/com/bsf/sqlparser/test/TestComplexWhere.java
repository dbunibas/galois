package com.bsf.sqlparser.test;

import com.bsf.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestComplexWhere {
    private static final String TABLE_NAME = "EmpTable";

    private static MainMemoryDB db;

    @BeforeAll
    public static void setUp() {
        String schema = requireNonNull(TestParseSelect.class.getResource("/employees/schema.xsd")).getFile();
        String instance = requireNonNull(TestParseSelect.class.getResource("/employees/50_emp.xml")).getFile();
        db = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testAndConditions() {
        String sql = String.format("select * from %s where name = 'TMDXWSCUHSE' and dept = 'DD31050'", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        logTuples(tuples);
    }

    @Test
    public void testMultipleAndConditions() {
        String sql = String.format("select * from %s t where t.name = 'TMDXWSCUHSE' and t.dept <> 'DD31051' and t.manager = 'NWNSALIYVAM' and t.salary >= 2000", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());
        logTuples(tuples);
    }

    @Test
    public void testOrConditions() {
        String sql = String.format("select * from %s where name = 'TMDXWSCUHSE' or dept = 'DD93665'", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        assertInstanceOf(Select.class, root);
        Select select = (Select) root;
        assertEquals(1, select.getSelections().size());

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(2, tuples.size());
        logTuples(tuples);
    }

    @Test
    public void testComplexConditions() {
        String sql = String.format("select t.name from %s t where (t.name = 'TMDXWSCUHSE' or t.name = 'VZXIEFGJBBE') or (t.dept = 'DD93665' and t.manager = 'CAFIDTXJRRM') order by t.name", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(3, tuples.size());

        assertEquals("HYHKEQTMHDE", tuples.get(0).getCells().get(1).getValue().getPrimitiveValue());
        assertEquals("TMDXWSCUHSE", tuples.get(1).getCells().get(1).getValue().getPrimitiveValue());
        assertEquals("VZXIEFGJBBE", tuples.get(2).getCells().get(1).getValue().getPrimitiveValue());

        logTuples(tuples);
    }

    private void logTuples(List<Tuple> tuples) {
        tuples.forEach(t -> log.info("{}", t));
    }
}
