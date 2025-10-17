package queryexecutor.model.database.operators.lazyloading;

import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.TupleOID;

public interface ITupleLoader {

    Tuple loadTuple();

    public TupleOID getOid();
}
