package engine.model.algebra;

import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.Tuple;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;

public class Offset extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(Offset.class);

    private long offset;

    public Offset(long size) {
        this.offset = size;
    }

    public String getName() {
        return "OFFSET " + offset;
    }

    public long getOffset() {
        return offset;
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        ITupleIterator leftTuples = children.get(0).execute(source, target);
        return new OffsetTupleIterator(leftTuples, offset);
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitOffset(this);
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.children.get(0).getAttributes(source, target);
    }
}

class OffsetTupleIterator implements ITupleIterator {

    private ITupleIterator tupleIterator;

    public OffsetTupleIterator(ITupleIterator tupleIterator, long offset) {
        this.tupleIterator = tupleIterator;
        for (int i = 0; i < offset; i++) {
            if (tupleIterator.hasNext()) {
                tupleIterator.next();
            }
        }
    }

    public void reset() {
        this.tupleIterator.reset();
    }

    public boolean hasNext() {
        return tupleIterator.hasNext();
    }

    public Tuple next() {
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
