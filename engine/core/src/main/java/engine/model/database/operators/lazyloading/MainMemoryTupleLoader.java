package engine.model.database.operators.lazyloading;

import engine.model.database.Tuple;
import engine.model.database.TupleOID;

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
