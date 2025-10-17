package queryexecutor.model.algebra.operators;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;

public interface IBatchInsert {

    void insert(ITable table, Tuple tuple, IDatabase database);
    
    void flush(IDatabase database);
}
