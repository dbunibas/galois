package engine.model.database.operators.lazyloading;

import java.util.Iterator;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.Tuple;

public class MainMemoryTupleLoaderIterator implements Iterator<ITupleLoader> {

    private final ITupleIterator tupleIterator;

    public MainMemoryTupleLoaderIterator(ITupleIterator tupleIterator) {
        this.tupleIterator = tupleIterator;
    }

    public boolean hasNext() {
        return tupleIterator.hasNext();
    }

    public MainMemoryTupleLoader next() {
        Tuple tuple = tupleIterator.next();
        return new MainMemoryTupleLoader(tuple);
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
