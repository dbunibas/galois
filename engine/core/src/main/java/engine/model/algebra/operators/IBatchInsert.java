package engine.model.algebra.operators;

import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;

public interface IBatchInsert {

    void insert(ITable table, Tuple tuple, IDatabase database);
    
    void flush(IDatabase database);
}
