package engine.model.algebra.operators;

import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;

public interface IInsertTuple {

    void execute(ITable table, Tuple tuple, IDatabase source, IDatabase target);

}
