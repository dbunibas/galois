package queryexecutor.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.OperatorFactory;
import queryexecutor.model.algebra.*;
import queryexecutor.model.algebra.aggregatefunctions.*;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.VirtualAttributeRef;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.model.expressions.Expression;
import queryexecutor.model.expressions.ExpressionAttributeRef;
import queryexecutor.persistence.DAOMainMemoryDatabase;
import queryexecutor.persistence.Types;
import queryexecutor.persistence.relational.QueryStatManager;
import queryexecutor.test.utility.UtilityForTests;
import queryexecutor.utility.QueryExecutorUtility;

import java.util.ArrayList;
import java.util.List;

public class TestMainMemory {

    private static Logger logger = LoggerFactory.getLogger(TestMainMemory.class);

    private MainMemoryDB database;
    private IRunQuery queryRunner;

    @Before
    public void setUp() {
        DAOMainMemoryDatabase daoDatabase = new DAOMainMemoryDatabase();
        String xsdSchema = "/employees/mainmemory/schema.xsd";
        String xmlInstance = "/employees/mainmemory/50_emp.xml";
//        String xsdSchema = "/bookpublisher/bookPublisher-schema.xsd";
//        String xmlInstance = "/bookpublisher/bookPublisher-instance.xml";
        database = daoDatabase.loadXMLDatabase(UtilityForTests.getAbsoluteFileName(xsdSchema), UtilityForTests.getAbsoluteFileName(xmlInstance));
        queryRunner = OperatorFactory.getInstance().getQueryRunner(database);
    }

    @Test
    public void testScan() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        ITupleIterator result = queryRunner.run(scan, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 50\n"));
    }

    @Test
    public void testIntersection1() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        Expression expression = new Expression("salary > 3000");
        expression.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select1 = new Select(expression);
        select1.addChild(scan);

        Expression expression2 = new Expression("salary > 2000");
        expression2.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select2 = new Select(expression2);
        select2.addChild(scan);

        Intersection intersection = new Intersection();
        intersection.addChild(select1);
        intersection.addChild(select2);
        if (logger.isDebugEnabled()) logger.debug(intersection.toString());
        ITupleIterator result = queryRunner.run(intersection, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 24\n"));
        QueryStatManager.getInstance().printStatistics();
    }
    
    @Test
    public void testIntersection2() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        Expression expression = new Expression("salary < 2000");
        expression.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select1 = new Select(expression);
        select1.addChild(scan);

        Expression expression2 = new Expression("manager == \"Paolo\"");
        expression2.changeVariableDescription("manager", new AttributeRef(tableAlias, "manager"));
        Select select2 = new Select(expression2);
        select2.addChild(scan);

        Intersection intersection = new Intersection();
        intersection.addChild(select1);
        intersection.addChild(select2);
        if (logger.isDebugEnabled()) logger.debug(intersection.toString());
        ITupleIterator result = queryRunner.run(intersection, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 1\n"));
        QueryStatManager.getInstance().printStatistics();
    }

    @Test
    public void testOrderBy() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        AttributeRef salary = new AttributeRef(tableAlias, "salary");
        OrderBy orderBy = new OrderBy(List.of(salary));
        orderBy.setOrder(OrderBy.ORDER_ASC);
        orderBy.addChild(scan);
        ITupleIterator result = queryRunner.run(orderBy, null, database);
        List<Tuple> orderedList = new ArrayList<>();
        while (result.hasNext()) {
            Tuple tuple = result.next();
            orderedList.add(tuple);
            logger.info(tuple.getCell(salary).getValue().toString());
        }
        result.close();
        logger.info("First tuple {}", orderedList.get(0));
        logger.info("Last tuple {}", orderedList.get(orderedList.size() - 1));
        Assert.assertTrue(orderedList.get(0).getCell(salary).getValue().toString().equals("1"));
        Assert.assertTrue(orderedList.get(orderedList.size() - 1).getCell(salary).getValue().toString().equals("100000"));
    }


    @Test
    public void testGroupByWithSort() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        AttributeRef salary = new AttributeRef(tableAlias, "salary");
        AttributeRef countAlias = new VirtualAttributeRef(tableAlias, "count", Types.INTEGER);
        AttributeRef maxAlias = new VirtualAttributeRef(tableAlias, "max", Types.INTEGER);
        AttributeRef minAlias = new VirtualAttributeRef(tableAlias, "min", Types.INTEGER);
        AttributeRef avgAlias = new VirtualAttributeRef(tableAlias, "avg", Types.REAL);
        AttributeRef dept = new AttributeRef(tableAlias, "dept");
        GroupBy groupBy = new GroupBy(List.of(
                dept),
                List.of(new ValueAggregateFunction(dept),
                        new CountAggregateFunction(salary, countAlias),
                        new MaxAggregateFunction(salary, maxAlias),
                        new MinAggregateFunction(salary, minAlias),
                        new AvgAggregateFunction(salary, avgAlias))
        );
        groupBy.addChild(scan);
        OrderBy orderBy = new OrderBy(List.of(countAlias));
        orderBy.setOrder(OrderBy.ORDER_DESC);
        orderBy.addChild(groupBy);
//        Limit limit = new Limit(1);
//        limit.addChild(orderBy);
        ITupleIterator result = queryRunner.run(orderBy, null, database);
        List<Tuple> orderedList = new ArrayList<>();
        while (result.hasNext()) {
            Tuple tuple = result.next();
            orderedList.add(tuple);
            logger.info(tuple.toString());
//            logger.info(tuple.getCell(salary).getValue().toString());
        }
        result.close();
//        logger.info("First tuple {}", orderedList.get(0));
//        logger.info("Last tuple {}", orderedList.get(orderedList.size() - 1));
//        Assert.assertTrue(orderedList.get(0).getCell(salary).getValue().toString().equals("1"));
//        Assert.assertTrue(orderedList.get(orderedList.size() - 1).getCell(salary).getValue().toString().equals("10000"));
    }

    @Test
    public void testMin() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        AttributeRef salary = new AttributeRef(tableAlias, "salary");
        AttributeRef min = new VirtualAttributeRef(tableAlias, "min", Types.INTEGER);
        Project project = new Project(List.of(new ProjectionAttribute(new MinAggregateFunction(salary, min))));
        project.addChild(scan);
        ITupleIterator result = queryRunner.run(project, null, database);
        List<Tuple> orderedList = new ArrayList<>();
        while (result.hasNext()) {
            Tuple tuple = result.next();
            orderedList.add(tuple);
            logger.info(tuple.getCell(min).getValue().toString());
        }
        result.close();
        logger.info("First tuple {}", orderedList.get(0));
        Assert.assertTrue(orderedList.get(0).getCell(min).getValue().toString().equals("1"));
    }

    @Test
    public void testProjectExpression() {
        TableAlias tableAlias = new TableAlias("EmpTable");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        AttributeRef salary = new AttributeRef(tableAlias, "salary");
        AttributeRef age = new AttributeRef(tableAlias, "age");
        AttributeRef manager = new AttributeRef(tableAlias, "manager");
        Expression expression = new Expression("salary - age");
        expression.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        expression.changeVariableDescription("age", new AttributeRef(tableAlias, "age"));
        AttributeRef expressionAttribute = new ExpressionAttributeRef(expression, tableAlias, "diff", Types.REAL);
        Project project = new Project(List.of(
                new ProjectionAttribute(manager),
                new ProjectionAttribute(age),
                new ProjectionAttribute(salary),
                new ProjectionAttribute(expressionAttribute)
        )
        );
        project.addChild(scan);
        ITupleIterator result = queryRunner.run(project, null, database);
        List<Tuple> orderedList = new ArrayList<>();
        while (result.hasNext()) {
            Tuple tuple = result.next();
            orderedList.add(tuple);
            logger.info("{}", tuple);
        }
        result.close();
        logger.info("First tuple {}", orderedList.get(0));
    }
}
