package engine.model.database.operators.lazyloading;

import engine.model.database.Tuple;
import engine.model.database.TupleOID;

public interface ITupleLoader {

    Tuple loadTuple();

    public TupleOID getOid();
}
