package queryexecutor.test.scalability;

import java.util.Date;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.OperatorFactory;
import queryexecutor.exceptions.DBMSException;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Join;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.InitDBConfiguration;
import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.persistence.DAODBMSDatabase;
import queryexecutor.persistence.file.CSVFile;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.test.utility.TestResults;
import queryexecutor.test.utility.UtilityForTests;
import queryexecutor.utility.Size;
import queryexecutor.utility.QueryExecutorUtility;

public class TestTPCHPart {

    private static Logger logger = LoggerFactory.getLogger(TestTPCHPart.class);

    private IRunQuery queryRunner;
    private boolean recreateDB = false;
    private boolean useIndex = false;

    @Test
    public void runMultiple() {
        TestResults.resetResults();
        useIndex = false;
        executeQuery(Size.S_1K, "NO-INDEX");
        executeQuery(Size.S_10K, "NO-INDEX");
        executeQuery(Size.S_50K, "NO-INDEX");
        executeQuery(Size.S_100K, "NO-INDEX");
        executeQuery(Size.S_200K, "NO-INDEX");
        useIndex = true;
        executeQuery(Size.S_1K, "WITH-INDEX");
        executeQuery(Size.S_10K, "WITH-INDEX");
        executeQuery(Size.S_50K, "WITH-INDEX");
        executeQuery(Size.S_100K, "WITH-INDEX");
        executeQuery(Size.S_200K, "WITH-INDEX");
        TestResults.printResults("Test_TPCHPart_Join");
    }

    private void executeQuery(Size size, String group) {
        IDatabase database = getDatabase(size);
        IAlgebraOperator operator = getQuery();
        long start = new Date().getTime();
        ITupleIterator result = queryRunner.run(operator, null, database);
        long resultSize = QueryExecutorUtility.getTupleIteratorSize(result);
        if (logger.isDebugEnabled()) logger.debug("Result size: " + resultSize);
        result.close();
        long end = new Date().getTime();
        long executionTime = end - start;
        if (logger.isDebugEnabled()) logger.debug("Test execution time: " + executionTime);
        TestResults.addTimeResult(size, group, executionTime);
        TestResults.printStats("\n****  Size: " + size.toString() + "  ****");
    }

    private IAlgebraOperator getQuery() {
        TableAlias tableAliasPart = new TableAlias("part");
        TableAlias tableAliasPartSupp = new TableAlias("partsupp");
        Scan scanPart = new Scan(tableAliasPart);
        Scan scanPartSupp = new Scan(tableAliasPartSupp);
        Join join = new Join(new AttributeRef(tableAliasPart, "p_partkey"), new AttributeRef(tableAliasPartSupp, "ps_partkey"));
        join.addChild(scanPart);
        join.addChild(scanPartSupp);
        return join;
    }

    private IDatabase getDatabase(Size size) {
        DAODBMSDatabase daoDatabase = new DAODBMSDatabase();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_tpch_dbgen_" + size.toString();
        String schema = "target";
        String login = "pguser";
        String password = "pguser";
        DBMSDB database = daoDatabase.loadDatabase(driver, uri, schema, login, password);
        String baseFolder = UtilityForTests.getAbsoluteFileName("/tpch-dbgen/");
        InitDBConfiguration initDBConfiguration = database.getInitDBConfiguration();
        initDBConfiguration.setCreateTablesFromFiles(true);
        initDBConfiguration.addFileToImportForTable("part", new CSVFile(baseFolder + "part.csv", '|', '"', size.getSize()));
        initDBConfiguration.addFileToImportForTable("partsupp", new CSVFile(baseFolder + "partsupp.csv", '|', '"', size.getSize()));
        queryRunner = OperatorFactory.getInstance().getQueryRunner(database);
        if (recreateDB) UtilityForTests.deleteDB(database.getAccessConfiguration());
        handleIndex(database);
        return database;
    }

    private void handleIndex(DBMSDB database) {
        StringBuilder createIndexQuery = new StringBuilder();
        createIndexQuery.append("DROP INDEX IF EXISTS \"target\".\"p_partkey_index\";");
        createIndexQuery.append("DROP INDEX IF EXISTS \"target\".\"ps_partkey_index\";");
        if (useIndex) {
            createIndexQuery.append("CREATE INDEX  \"p_partkey_index\" ON \"target\".\"part\" USING btree(p_partkey);");
            createIndexQuery.append("CREATE INDEX  \"ps_partkey_index\" ON \"target\".\"partsupp\" USING btree(ps_partkey);");
        }
        createIndexQuery.append("VACUUM ANALYZE \"target\".\"part\";");
        createIndexQuery.append("VACUUM ANALYZE  \"target\".\"partsupp\";");
        try {
            QueryManager.executeScript(createIndexQuery.toString(), database.getAccessConfiguration(), true, true, false, true);
        } catch (DBMSException sqle) {
            logger.warn("Unable to execute script " + sqle);

        }
    }
}
