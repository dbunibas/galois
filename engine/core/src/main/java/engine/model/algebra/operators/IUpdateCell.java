package engine.model.algebra.operators;

import engine.model.database.CellRef;
import engine.model.database.IDatabase;
import engine.model.database.IValue;

public interface IUpdateCell {

    void execute(CellRef cellRef, IValue value, IDatabase database);

}
