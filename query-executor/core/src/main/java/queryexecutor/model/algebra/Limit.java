package queryexecutor.model.algebra;

import queryexecutor.exceptions.AlgebraException;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;

public class Limit extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(Limit.class);

    private long size;

    public Limit(long size) {
        this.size = size;
    }

    public String getName() {
        return "LIMIT " + size;
    }

    public long getSize() {
        return size;
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        ITupleIterator leftTuples = children.get(0).execute(source, target);
        return new LimitTupleIterator(leftTuples, size);
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitLimit(this);
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.children.get(0).getAttributes(source, target);
    }
}

class LimitTupleIterator implements ITupleIterator {

    private ITupleIterator tupleIterator;
    private long read = 0;
    private long limit;

    public LimitTupleIterator(ITupleIterator tupleIterator, long limit) {
        this.tupleIterator = tupleIterator;
        this.limit = limit;
    }

    public void reset() {
        this.tupleIterator.reset();
        this.read = 0;
    }

    public boolean hasNext() {
        return tupleIterator.hasNext() && read < limit;
    }

    public Tuple next() {
        if (read >= limit) {
            throw new AlgebraException("No more elements in limit");
        }
        read++;
        return tupleIterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public int size() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void close() {
        tupleIterator.close();
    }
}
