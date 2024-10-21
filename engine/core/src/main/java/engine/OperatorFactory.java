package engine;

import engine.model.database.operators.IRunQuery;
import engine.model.database.operators.dbms.SQLRunQuery;
import engine.model.database.operators.mainmemory.MainMemoryRunQuery;
import engine.model.algebra.operators.IInsertTuple;
import engine.model.algebra.operators.IUpdateCell;
import engine.model.algebra.operators.mainmemory.MainMemoryInsertTuple;
import engine.model.algebra.operators.mainmemory.MainMemoryUpdateCell;
import engine.model.algebra.operators.sql.SQLInsertTuple;
import engine.model.algebra.operators.sql.SQLUpdateCell;
import engine.model.database.operators.IDatabaseManager;
import engine.model.database.operators.IExplainQuery;
import engine.model.database.operators.dbms.SQLDatabaseManager;
import engine.model.database.operators.dbms.SQLExplainQuery;
import engine.model.database.operators.mainmemory.MainMemoryDatabaseManager;
import engine.model.database.operators.mainmemory.MainMemoryExplainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.operators.IBatchInsert;
import engine.model.algebra.operators.ICreateTable;
import engine.model.algebra.operators.IDelete;
import engine.model.algebra.operators.mainmemory.MainMemoryBatchInsert;
import engine.model.algebra.operators.mainmemory.MainMemoryCreateTable;
import engine.model.algebra.operators.mainmemory.MainMemoryDelete;
import engine.model.algebra.operators.sql.SQLBatchInsert;
import engine.model.algebra.operators.sql.SQLCreateTable;
import engine.model.algebra.operators.sql.SQLDelete;
import engine.model.database.IDatabase;
import engine.model.database.mainmemory.MainMemoryDB;
import engine.model.database.operators.IOIDGenerator;
import engine.model.database.operators.dbms.SQLOIDGenerator;
import engine.model.database.operators.mainmemory.MainMemoryOIDGenerator;

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
