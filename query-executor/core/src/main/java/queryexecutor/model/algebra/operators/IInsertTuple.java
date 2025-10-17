package queryexecutor.model.algebra.operators;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;

public interface IInsertTuple {

    void execute(ITable table, Tuple tuple, IDatabase source, IDatabase target);

}
