package queryexecutor.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.OperatorFactory;
import queryexecutor.model.algebra.Join;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.model.expressions.Expression;
import queryexecutor.persistence.DAODBMSDatabase;
import queryexecutor.persistence.file.XMLFile;
import queryexecutor.persistence.relational.QueryStatManager;
import queryexecutor.test.utility.UtilityForTests;
import queryexecutor.utility.QueryExecutorUtility;

public class TestDBMSXML {

    private static Logger logger = LoggerFactory.getLogger(TestDBMSXML.class);

    private DBMSDB database;
    private IRunQuery queryRunner;

    @Before
    public void setUp() {
        DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_employees";
        String schema = "target";
        String login = "pguser";
        String password = "pguser";
        database = daoDatabase.loadDatabase(driver, uri, schema, login, password);
        database.getInitDBConfiguration().setCreateTablesFromFiles(true);
        XMLFile fileToImport = new XMLFile(UtilityForTests.getAbsoluteFileName("/employees/xml/50_emp.xml"));
        database.getInitDBConfiguration().addFileToImportForTable("emp", fileToImport);
        UtilityForTests.deleteDB(database.getAccessConfiguration());
        queryRunner = OperatorFactory.getInstance().getQueryRunner(database);
    }

    @Test
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
    public void testSelect() {
        TableAlias tableAlias = new TableAlias("emp");
        Scan scan = new Scan(tableAlias);
        Expression expression = new Expression("name == \"Paolo\"");
        expression.changeVariableDescription("name", new AttributeRef(tableAlias, "name"));
        Select select = new Select(expression);
        select.addChild(scan);
        if (logger.isDebugEnabled()) logger.debug(select.toString());
        ITupleIterator result = queryRunner.run(select, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 1\n"));
        QueryStatManager.getInstance().printStatistics();
    }

    @Test
    public void testJoin() {
        TableAlias tableAlias1 = new TableAlias("emp", "1");
        Scan scan1 = new Scan(tableAlias1);
        TableAlias tableAlias2 = new TableAlias("emp", "2");
        Scan scan2 = new Scan(tableAlias2);
        Join join = new Join(new AttributeRef(tableAlias1, "name"), new AttributeRef(tableAlias2, "manager"));
        join.addChild(scan1);
        join.addChild(scan2);
        if (logger.isDebugEnabled()) logger.debug(join.toString());
        ITupleIterator result = queryRunner.run(join, null, database);
        String stringResult = QueryExecutorUtility.printTupleIterator(result);
        if (logger.isDebugEnabled()) logger.debug(stringResult);
        result.close();
        Assert.assertTrue(stringResult.startsWith("Number of tuples: 2\n"));
        QueryStatManager.getInstance().printStatistics();
    }
}
