package queryexecutor.model.algebra.operators.mainmemory;

import queryexecutor.model.algebra.operators.IBatchInsert;
import queryexecutor.model.algebra.operators.IInsertTuple;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;

public class MainMemoryBatchInsert implements IBatchInsert {

    private IInsertTuple insertTuple = new MainMemoryInsertTuple();

    public void insert(ITable table, Tuple tuple, IDatabase database) {
        insertTuple.execute(table, tuple, null, database);
    }

    public void flush(IDatabase database) {
    }

}
