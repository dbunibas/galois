package galois.test;

import galois.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.*;
import speedy.model.algebra.aggregatefunctions.AvgAggregateFunction;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.model.expressions.Expression;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class TestSpeedyAlgebra {
    private static final String TABLE_NAME = "EmpTable";

    private static MainMemoryDB database;

    @BeforeAll
    public static void setUp() {
        String schema = TestSpeedyAlgebra.class.getResource("/employees/mainmemory/schema.xsd").getFile();
        String instance = TestSpeedyAlgebra.class.getResource("/employees/mainmemory/50_emp.xml").getFile();
        database = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testEmployeesTable() {
        ITable table = database.getTable(TABLE_NAME);
        Assertions.assertNotNull(table, "Table is null!");
    }

    @Test
    public void testSimpleSelectFrom() {
        // SQL: SELECT * FROM table
        ITable table = database.getTable(TABLE_NAME);
        Assertions.assertNotNull(table, "Table is null!");

        TableAlias tableAlias = new TableAlias(TABLE_NAME);
        Scan scan = new Scan(tableAlias);

        ITupleIterator iterator = scan.execute(null, database);
        Stream<Tuple> stream = TestUtils.toTupleStream(iterator);
        Assertions.assertEquals(table.getSize(), stream.count());
    }

    @Test
    public void testSimpleSelectFromWhere() {
        // SQL: SELECT * FROM table t WHERE t.name = "TMDXWSCUHSE"
        TableAlias tableAlias = new TableAlias(TABLE_NAME);
        Scan scan = new Scan(tableAlias);
        Expression exp = new Expression("name == \"TMDXWSCUHSE\"");
        exp.setVariableDescription("name", new AttributeRef(tableAlias, "name"));
        Select select = new Select(exp);
        select.addChild(scan);

        ITupleIterator iterator = select.execute(null, database);
        Stream<Tuple> stream = TestUtils.toTupleStream(iterator);
        Assertions.assertEquals(1, stream.count());
    }

    @Test
    public void testOtherSelectFromWhere() {
        // SQL: SELECT * FROM table t WHERE t.salary > 3500
        TableAlias tableAlias = new TableAlias(TABLE_NAME);
        Scan scan = new Scan(tableAlias);
        Expression exp = new Expression("salary > 3500");
        exp.setVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select = new Select(exp);
        select.addChild(scan);

        ITupleIterator iterator = select.execute(null, database);
        Stream<Tuple> stream = TestUtils.toTupleStream(iterator);
        Assertions.assertEquals(16, stream.count());
    }

    @Test
    public void testAverage() {
        //SQL: SELECT avg(income) FROM table t WHERE t.salary BETWEEN 4000 AND 5000;
        TableAlias tableAlias = new TableAlias(TABLE_NAME);
        Scan scan = new Scan(tableAlias);
        Expression exp = new Expression("salary > 4000 && salary < 5000");
        AttributeRef salaryRef = new AttributeRef(tableAlias, "salary");
        exp.setVariableDescription("salary", salaryRef);
        Select select = new Select(exp);
        select.addChild(scan);
        AvgAggregateFunction avgFunction = new AvgAggregateFunction(salaryRef);
        ProjectionAttribute avgAttribute = new ProjectionAttribute(avgFunction);
        Project project = new Project(Collections.singletonList(avgAttribute));
        project.addChild(select);

        ITupleIterator iterator = project.execute(null, database);
        Tuple result = TestUtils.toTupleStream(iterator).findFirst().orElse(null);
        Assertions.assertNotNull(result, "Result is null!");
        double value = Double.parseDouble((String) result.getCell(salaryRef).getValue().getPrimitiveValue());
        Assertions.assertTrue(Math.abs(4309.11111111 - value) < 0.0001, "Inconsistent average!");
    }

    @Test
    public void testDistinct() {
        // SQL: SELECT DISTINCT(t.name) FROM table t
        TableAlias tableAlias = new TableAlias(TABLE_NAME);
        Scan scan = new Scan(tableAlias);
        ProjectionAttribute projectionAttribute = new ProjectionAttribute(new AttributeRef(tableAlias, "name"));
        Project project = new Project(Collections.singletonList(projectionAttribute));
        project.addChild(scan);
        Distinct distinct = new Distinct();
        distinct.addChild(project);

        ITupleIterator iterator = distinct.execute(null, database);
        Stream<Tuple> stream = TestUtils.toTupleStream(iterator);
        Assertions.assertEquals(47, stream.count());
    }

    @Test
    public void testJoin() {
        // SQL: SELECT * FORM table t1 JOIN table t2 on t1.name = t2.manager
        TableAlias t1 = new TableAlias(TABLE_NAME, "t1");
        TableAlias t2 = new TableAlias(TABLE_NAME, "t2");
        Scan scan1 = new Scan(t1);
        Scan scan2 = new Scan(t2);

        List<AttributeRef> leftAttributes = List.of(new AttributeRef(t1, "name"));
        List<AttributeRef> rightAttributes = List.of(new AttributeRef(t2, "manager"));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(scan1);
        join.addChild(scan2);

        ITupleIterator iterator = join.execute(null, database);
        Stream<Tuple> stream = TestUtils.toTupleStream(iterator);
        Assertions.assertEquals(2, stream.count());
    }
}
