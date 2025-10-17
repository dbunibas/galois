package queryexecutor.test;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.OperatorFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.algebra.GroupBy;
import queryexecutor.model.algebra.Intersection;
import queryexecutor.model.algebra.Join;
import queryexecutor.model.algebra.Limit;
import queryexecutor.model.algebra.OrderBy;
import queryexecutor.model.algebra.OrderByRandom;
import queryexecutor.model.algebra.Project;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.algebra.aggregatefunctions.CountAggregateFunction;
import queryexecutor.model.algebra.aggregatefunctions.IAggregateFunction;
import queryexecutor.model.algebra.aggregatefunctions.ValueAggregateFunction;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.model.expressions.Expression;
import queryexecutor.persistence.DAODBMSDatabase;
import queryexecutor.persistence.file.CSVFile;
import queryexecutor.persistence.file.operators.ExportCSVFileWithCopy;
import queryexecutor.persistence.relational.QueryStatManager;
import queryexecutor.test.utility.UtilityForTests;
import queryexecutor.utility.QueryExecutorUtility;

public class TestElection {

    private static Logger logger = LoggerFactory.getLogger(TestDBMSCSV.class);

    private DBMSDB database;
    private IRunQuery queryRunner;

    @Before
    public void setUp() {
        DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_tpch_dbgen_1k";
        String schema = "target";
        String login = "pguser";
        String password = "pguser";
        database = daoDatabase.loadDatabase(driver, uri, schema, login, password);
        database.getInitDBConfiguration().setCreateTablesFromFiles(true);
//        database.getInitDBConfiguration().setUseCopyStatement(false);
        CSVFile fileToImport = new CSVFile(UtilityForTests.getAbsoluteFileName("/employees/csv/50_emp.csv"));
        fileToImport.setSeparator(',');
        database.getInitDBConfiguration().addFileToImportForTable("emp", fileToImport);
//        UtilityForTests.deleteDB(database.getAccessConfiguration());
        database.initDBMS();
        queryRunner = OperatorFactory.getInstance().getQueryRunner(database);
    }

    @After
    public void tearDown() {
        UtilityForTests.deleteDB(database.getAccessConfiguration());
    }

//    @Test
    public void testImport() {
        Assert.assertEquals(1, database.getTableNames().size());
        Assert.assertEquals(50, database.getTable("emp").getSize());
        ExportCSVFileWithCopy exporter = new ExportCSVFileWithCopy();
        exporter.exportDatabase(database, null, true, "/Temp/QueryExecutor/Export", 1);
    }

//    @Test
    public void xtestOrderByGroupBySelect() {
        TableAlias tableAlias = new TableAlias("part");
        Scan scan = new Scan(tableAlias);
        AttributeRef attributeRef = new AttributeRef(tableAlias, "p_name");
        Expression expression = new Expression("p_size > 20"); //
        expression.changeVariableDescription("p_size", new AttributeRef(tableAlias, "p_size")); //
        Select select = new Select(expression); //
        select.addChild(scan); //
        List<AttributeRef> groupingAttribute = new ArrayList<AttributeRef>();
        groupingAttribute.add(attributeRef);
        List<IAggregateFunction> aggregateFunctions = new ArrayList<IAggregateFunction>();
        aggregateFunctions.add(new ValueAggregateFunction(attributeRef));
        AttributeRef attributeCount = new AttributeRef(tableAlias, QueryExecutorConstants.COUNT);
        CountAggregateFunction countAggregateFunction = new CountAggregateFunction(attributeCount);
        aggregateFunctions.add(countAggregateFunction);
        GroupBy groupBy = new GroupBy(groupingAttribute, aggregateFunctions);
        groupBy.addChild(select);
        List<AttributeRef> countAttributeRef = new ArrayList<AttributeRef>();
        countAttributeRef.add(attributeCount);
        OrderBy orderBy = new OrderBy(countAttributeRef);
        orderBy.setOrder(OrderBy.ORDER_DESC);
        orderBy.addChild(groupBy);
        Limit limit = new Limit(10);
        limit.addChild(orderBy);
        if (logger.isDebugEnabled()) logger.debug(limit.toString());
        ITupleIterator result = queryRunner.run(limit, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 10\n"));
    }

//    @Test
    public void testOrderByGroupBy() {
        TableAlias tableAlias = new TableAlias("part");
        Scan scan = new Scan(tableAlias);
        AttributeRef attributeRef = new AttributeRef(tableAlias, "p_name");

        List<AttributeRef> groupingAttribute = new ArrayList<AttributeRef>();
        groupingAttribute.add(attributeRef);
        List<IAggregateFunction> aggregateFunctions = new ArrayList<IAggregateFunction>();
        aggregateFunctions.add(new ValueAggregateFunction(attributeRef));
        AttributeRef attributeCount = new AttributeRef(tableAlias, QueryExecutorConstants.COUNT);
        CountAggregateFunction countAggregateFunction = new CountAggregateFunction(attributeCount);
        aggregateFunctions.add(countAggregateFunction);
        GroupBy groupBy = new GroupBy(groupingAttribute, aggregateFunctions);
        groupBy.addChild(scan);
        List<AttributeRef> countAttributeRef = new ArrayList<AttributeRef>();
        countAttributeRef.add(attributeCount);
        OrderBy orderBy = new OrderBy(countAttributeRef);
        orderBy.setOrder(OrderBy.ORDER_DESC);
        orderBy.addChild(groupBy);
        Limit limit = new Limit(10);
        limit.addChild(orderBy);
        if (logger.isDebugEnabled()) logger.debug(limit.toString());
        ITupleIterator result = queryRunner.run(limit, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 10\n"));
    }

//    @Test
    public void testOrderByGroupByWithJoinSelect() {
        TableAlias tableAliasPart = new TableAlias("part");
        TableAlias tableAliasPartSupp = new TableAlias("partsupp");
        Scan scanPart = new Scan(tableAliasPart);
        Scan scanPartSupp = new Scan(tableAliasPartSupp);
        Expression expression = new Expression("p_size > 20"); //
        expression.changeVariableDescription("p_size", new AttributeRef(tableAliasPart, "p_size")); //
        Select select = new Select(expression); //
        select.addChild(scanPart); //
        List<AttributeRef> leftAttributes = new ArrayList<AttributeRef>();
        leftAttributes.add(new AttributeRef(tableAliasPart, "p_partkey"));
        List<AttributeRef> rightAttributes = new ArrayList<AttributeRef>();
        rightAttributes.add(new AttributeRef(tableAliasPartSupp, "ps_partkey"));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(select);
        join.addChild(scanPartSupp);
        AttributeRef attributeRef = new AttributeRef(tableAliasPart, "p_name");
        List<AttributeRef> groupingAttribute = new ArrayList<AttributeRef>();
        groupingAttribute.add(attributeRef);
        List<IAggregateFunction> aggregateFunctions = new ArrayList<IAggregateFunction>();
        aggregateFunctions.add(new ValueAggregateFunction(attributeRef));
        AttributeRef attributeCount = new AttributeRef(tableAliasPart, QueryExecutorConstants.COUNT);
        CountAggregateFunction countAggregateFunction = new CountAggregateFunction(attributeCount);
        aggregateFunctions.add(countAggregateFunction);
        GroupBy groupBy = new GroupBy(groupingAttribute, aggregateFunctions);
        groupBy.addChild(join);
        List<AttributeRef> countAttributeRef = new ArrayList<AttributeRef>();
        countAttributeRef.add(attributeCount);
        OrderBy orderBy = new OrderBy(countAttributeRef);
        orderBy.setOrder(OrderBy.ORDER_DESC);
        orderBy.addChild(groupBy);
        Limit limit = new Limit(10);
        limit.addChild(orderBy);
        if (logger.isDebugEnabled()) {
            logger.debug(limit.toString());
        }
        ITupleIterator result = queryRunner.run(limit, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) {
            logger.debug(stringResult);
        }
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 10\n"));
    }

//    @Test
    public void testDoubleSelect() {
        TableAlias tableAliasPart = new TableAlias("part");
        TableAlias tableAliasPartSupp = new TableAlias("partsupp");
        Scan scanPart = new Scan(tableAliasPart);
        Scan scanPartSupp = new Scan(tableAliasPartSupp);
        Expression expression = new Expression("p_size > 20"); //
        expression.changeVariableDescription("p_size", new AttributeRef(tableAliasPart, "p_size")); //
        Select select = new Select(expression); //
        select.addChild(scanPart); //
        Expression expression2 = new Expression("p_container == \"JUMBO BOX\""); //
        expression2.changeVariableDescription("p_container", new AttributeRef(tableAliasPart, "p_container")); //
        Select select2 = new Select(expression2); //
        select2.addChild(select); //
        List<AttributeRef> leftAttributes = new ArrayList<AttributeRef>();
        leftAttributes.add(new AttributeRef(tableAliasPart, "p_partkey"));
        List<AttributeRef> rightAttributes = new ArrayList<AttributeRef>();
        rightAttributes.add(new AttributeRef(tableAliasPartSupp, "ps_partkey"));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(select2);
        join.addChild(scanPartSupp);
        if (logger.isInfoEnabled()) logger.info(join.toString());
        ITupleIterator result = queryRunner.run(join, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isInfoEnabled()) logger.info(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 16\n"));
    }

//    @Test
    public void testScan() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        if (logger.isDebugEnabled()) logger.debug(scan.toString());
        ITupleIterator result = queryRunner.run(scan, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 50\n"));
    }

    @Test
    public void testOne() {
        
    }
    
    @Test
    public void testTwo() {
        
    }
//    @Test
    public void testSelect() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        Expression expression = new Expression("salary > 3000");
        expression.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select = new Select(expression);
        select.addChild(scan);
        if (logger.isDebugEnabled()) logger.debug(select.toString());
        ITupleIterator result = queryRunner.run(select, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 24\n"));
        QueryStatManager.getInstance().printStatistics();
    }

//    @Test
    public void testRandom() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        OrderByRandom random = new OrderByRandom();
        random.addChild(scan);
        Limit limit = new Limit(10);
        limit.addChild(random);
        if (logger.isDebugEnabled()) logger.debug(limit.toString());
        ITupleIterator result = queryRunner.run(limit, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 10\n"));
    }

//    @Test
    public void testHaving() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        List<AttributeRef> groupingAttribute = new ArrayList<AttributeRef>();
        AttributeRef nameAttribute = new AttributeRef(tableAlias, "name");
        groupingAttribute.add(nameAttribute);
        List<IAggregateFunction> aggregateFunctions = new ArrayList<IAggregateFunction>();
        aggregateFunctions.add(new ValueAggregateFunction(nameAttribute));
        AttributeRef countAttribute = new AttributeRef(new TableAlias(QueryExecutorConstants.AGGR), QueryExecutorConstants.COUNT);
        aggregateFunctions.add(new CountAggregateFunction(countAttribute));
        GroupBy groupBy = new GroupBy(groupingAttribute, aggregateFunctions);
        groupBy.addChild(scan);
        Expression expression = new Expression("count > 1");
        expression.changeVariableDescription("count", countAttribute);
        Select select = new Select(expression);
        select.addChild(groupBy);
        Project project = new Project(QueryExecutorUtility.createProjectionAttributes(groupingAttribute));
        project.addChild(select);
        if (logger.isInfoEnabled()) logger.info(project.toString());
        ITupleIterator result = queryRunner.run(project, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isInfoEnabled()) logger.info(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 3\n"));
    }
    
    @Test
    public void Intersection() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        Expression expression = new Expression("salary > 3000");
        expression.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select1 = new Select(expression);
        select1.addChild(scan);
        if (logger.isDebugEnabled()) logger.debug(select1.toString());
        ITupleIterator result1 = queryRunner.run(select1, null, database);
        String stringResult1 = QueryExecutorUtility.printTupleIterator(result1);
        if (logger.isDebugEnabled()) logger.debug(stringResult1);
        result1.close();
        Assert.assertTrue(stringResult1.startsWith("Number of tuples: 24\n"));
        QueryStatManager.getInstance().printStatistics();
        
        Expression expression2 = new Expression("salary > 2000");
        expression2.changeVariableDescription("salary", new AttributeRef(tableAlias, "salary"));
        Select select2 = new Select(expression2);
        select2.addChild(scan);
        if (logger.isDebugEnabled()) logger.debug(select2.toString());
        ITupleIterator result2 = queryRunner.run(select2, null, database);
        String stringResult2 = QueryExecutorUtility.printTupleIterator(result2);
        if (logger.isDebugEnabled()) logger.debug(stringResult1);
        result2.close();
        Assert.assertTrue(stringResult2.startsWith("Number of tuples: 31\n"));
        QueryStatManager.getInstance().printStatistics();
        
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

}
