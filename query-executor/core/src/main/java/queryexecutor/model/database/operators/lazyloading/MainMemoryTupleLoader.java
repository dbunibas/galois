package queryexecutor.model.database.operators.lazyloading;

import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.TupleOID;

public class MainMemoryTupleLoader implements ITupleLoader {

    private final Tuple tuple;

    public MainMemoryTupleLoader(Tuple tuple) {
        this.tuple = tuple;
    }

    @Override
    public Tuple loadTuple() {
        return tuple;
    }

    public TupleOID getOid() {
        return tuple.getOid();
    }
}
