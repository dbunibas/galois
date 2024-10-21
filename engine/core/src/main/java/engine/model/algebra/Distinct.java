package engine.model.algebra;

import engine.utility.EngineUtility;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.algebra.operators.ListTupleIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.Tuple;

public class Distinct extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(Distinct.class);

    @Override
    public String getName() {
        return "DISTINCT";
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.children.get(0).getAttributes(source, target);
    }

    @Override
    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitDistinct(this);
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        ITupleIterator it = children.get(0).execute(source, target);
        List<Tuple> materializedTuples = materialize(it);
        removeDuplicates(materializedTuples);
        ITupleIterator result = new ListTupleIterator(materializedTuples);
        if (logger.isDebugEnabled()) logger.debug("Executing select: " + getName() + " on tuples:\n" + EngineUtility.printIteratorAndReset(children.get(0).execute(source, target)));
        if (logger.isDebugEnabled()) logger.debug("Result:\n" + EngineUtility.printIteratorAndReset(result));
        if (logger.isDebugEnabled()) result.reset();
        return result;
    }

    private List<Tuple> materialize(ITupleIterator it) {
        List<Tuple> result = new ArrayList<Tuple>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        it.close();
        return result;
    }

    private void removeDuplicates(List<Tuple> materializedTuples) {
        Collections.sort(materializedTuples, new TupleComparatorNoOID());
        String lastTupleString = "";
        for (Iterator<Tuple> it = materializedTuples.iterator(); it.hasNext();) {
            Tuple tuple = it.next();
            String tupleString = tuple.toStringNoOID();
            if (tupleString.equals(lastTupleString)) {
                it.remove();
            } else {
                lastTupleString = tupleString;
            }
        }
    }
}

class TupleComparatorNoOID implements Comparator<Tuple> {

    public int compare(Tuple t1, Tuple t2) {
        return t1.toStringNoOID().compareTo(t2.toStringNoOID());
    }
}