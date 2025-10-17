package queryexecutor.model.algebra.operators;

import queryexecutor.model.database.Tuple;
import java.util.Iterator;

public interface ITupleIterator extends Iterator<Tuple> {

    public void reset();

    public void close();

}
