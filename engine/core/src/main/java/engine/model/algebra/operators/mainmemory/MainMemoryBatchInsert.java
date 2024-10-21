package engine.model.algebra.operators.mainmemory;

import engine.model.algebra.operators.IBatchInsert;
import engine.model.algebra.operators.IInsertTuple;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;

public class MainMemoryBatchInsert implements IBatchInsert {

    private IInsertTuple insertTuple = new MainMemoryInsertTuple();

    public void insert(ITable table, Tuple tuple, IDatabase database) {
        insertTuple.execute(table, tuple, null, database);
    }

    public void flush(IDatabase database) {
    }

}
