package queryexecutor;

import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.model.database.operators.dbms.SQLRunQuery;
import queryexecutor.model.database.operators.mainmemory.MainMemoryRunQuery;
import queryexecutor.model.algebra.operators.IInsertTuple;
import queryexecutor.model.algebra.operators.IUpdateCell;
import queryexecutor.model.algebra.operators.mainmemory.MainMemoryInsertTuple;
import queryexecutor.model.algebra.operators.mainmemory.MainMemoryUpdateCell;
import queryexecutor.model.algebra.operators.sql.SQLInsertTuple;
import queryexecutor.model.algebra.operators.sql.SQLUpdateCell;
import queryexecutor.model.database.operators.IDatabaseManager;
import queryexecutor.model.database.operators.IExplainQuery;
import queryexecutor.model.database.operators.dbms.SQLDatabaseManager;
import queryexecutor.model.database.operators.dbms.SQLExplainQuery;
import queryexecutor.model.database.operators.mainmemory.MainMemoryDatabaseManager;
import queryexecutor.model.database.operators.mainmemory.MainMemoryExplainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.operators.IBatchInsert;
import queryexecutor.model.algebra.operators.ICreateTable;
import queryexecutor.model.algebra.operators.IDelete;
import queryexecutor.model.algebra.operators.mainmemory.MainMemoryBatchInsert;
import queryexecutor.model.algebra.operators.mainmemory.MainMemoryCreateTable;
import queryexecutor.model.algebra.operators.mainmemory.MainMemoryDelete;
import queryexecutor.model.algebra.operators.sql.SQLBatchInsert;
import queryexecutor.model.algebra.operators.sql.SQLCreateTable;
import queryexecutor.model.algebra.operators.sql.SQLDelete;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.model.database.operators.IOIDGenerator;
import queryexecutor.model.database.operators.dbms.SQLOIDGenerator;
import queryexecutor.model.database.operators.mainmemory.MainMemoryOIDGenerator;

public class OperatorFactory {

    private static Logger logger = LoggerFactory.getLogger(OperatorFactory.class);
    private static OperatorFactory singleton = new OperatorFactory();
    //
    private IRunQuery mainMemoryQueryRunner = new MainMemoryRunQuery();
    private IRunQuery sqlQueryRunner = new SQLRunQuery();
    //
    private IExplainQuery mainMemoryQueryExplanator = new MainMemoryExplainQuery();
    private IExplainQuery sqlQueryExplanator = new SQLExplainQuery();
    //
    private IUpdateCell mainMemoryCellUpdater = new MainMemoryUpdateCell();
    private IUpdateCell sqlCellUpdater = new SQLUpdateCell();
    //
    private IInsertTuple mainMemoryInsertOperator = new MainMemoryInsertTuple();
    private IInsertTuple sqlInsertOperator = new SQLInsertTuple();
    //
    private IDatabaseManager mainMemoryDatabaseManager = new MainMemoryDatabaseManager();
    private IDatabaseManager sqlDatabaseManager = new SQLDatabaseManager();
    //
    private ICreateTable mainMemoryTableCreator = new MainMemoryCreateTable();
    private ICreateTable sqlTableCreator = new SQLCreateTable();
    //
    private IDelete mainMemoryDeleteOperator = new MainMemoryDelete();
    private IDelete sqlDeleteOperator = new SQLDelete();
    //
    private IBatchInsert mainMemoryBatchInsertOperator = new MainMemoryBatchInsert();
    private IBatchInsert sqlBatchInsertOperator  = new SQLBatchInsert();
    //
    private IOIDGenerator mainMemoryOIDGenerator = new MainMemoryOIDGenerator();
    private IOIDGenerator sqlOIDGenerator = SQLOIDGenerator.getInstance();

    private OperatorFactory() {
    }

    public static OperatorFactory getInstance() {
        return singleton;
    }

    public IRunQuery getQueryRunner(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryQueryRunner;
        }
        return sqlQueryRunner;
    }

    public IUpdateCell getCellUpdater(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryCellUpdater;
        }
        return sqlCellUpdater;
    }

    public IExplainQuery getQueryExplanator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryQueryExplanator;
        }
        return sqlQueryExplanator;
    }

    public IDatabaseManager getDatabaseManager(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryDatabaseManager;
        }
        return sqlDatabaseManager;
    }

    public IInsertTuple getInsertOperator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryInsertOperator;
        }
        return sqlInsertOperator;
    }

    public IDelete getDeleteOperator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryDeleteOperator;
        }
        return sqlDeleteOperator;
    }

    public IBatchInsert getSingletonBatchInsertOperator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryBatchInsertOperator;
        }
        return sqlBatchInsertOperator;
    }

    public IBatchInsert getNonSingletonBatchInsertOperator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return new MainMemoryBatchInsert();
        }
        return new SQLBatchInsert();
    }

    public IOIDGenerator getOIDGenerator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryOIDGenerator;
        }
        return sqlOIDGenerator;
    }

    public ICreateTable getTableCreator(IDatabase database) {
        if (this.isMainMemory(database)) {
            return mainMemoryTableCreator;
        }
        return sqlTableCreator;
    }

    private boolean isMainMemory(IDatabase database) {
        return (database instanceof MainMemoryDB);
    }
}
