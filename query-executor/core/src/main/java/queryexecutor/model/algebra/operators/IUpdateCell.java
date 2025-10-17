package queryexecutor.model.algebra.operators;

import queryexecutor.model.database.CellRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.IValue;

public interface IUpdateCell {

    void execute(CellRef cellRef, IValue value, IDatabase database);

}
