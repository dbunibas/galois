package galois.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestSpeedyAlgebra {

    private static final String TABLE_NAME = "EmpTable";
    private static final Logger logger = LoggerFactory.getLogger(TestSpeedyAlgebra.class);

    private static MainMemoryDB database;

    @BeforeAll
    public static void setUp() {
//        String schema = UtilityForTests.getAbsoluteFileName("employees/mainmemory/schema.xsd");
//        String instance = UtilityForTests.getAbsoluteFileName("employees/mainmemory/50_emp.xml");
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
        Stream<Tuple> stream = toTupleStream(iterator);
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
        Stream<Tuple> stream = toTupleStream(iterator);
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
        Stream<Tuple> stream = toTupleStream(iterator);
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
        Tuple result = toTupleStream(iterator).findFirst().orElse(null);
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
        Stream<Tuple> stream = toTupleStream(iterator);
        Assertions.assertEquals(47, stream.count());
    }

    private Stream<Tuple> toTupleStream(ITupleIterator iterator) {
        Iterable<Tuple> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
