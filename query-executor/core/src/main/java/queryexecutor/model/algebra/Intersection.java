package queryexecutor.model.algebra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.algebra.operators.ListTupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;
import queryexecutor.utility.AlgebraUtility;

public class Intersection extends AbstractOperator {

    @Override
    public String getName() {
        return "INTERSECT";
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        List<Tuple> result = new ArrayList<Tuple>();
        ITupleIterator leftTuples = children.get(0).execute(source, target);
        ITupleIterator rightTuples = children.get(1).execute(source, target);
        materializeResult(leftTuples, rightTuples, result);
        leftTuples.close();
        rightTuples.close();
        return new ListTupleIterator(result);
    }

    private void materializeResult(ITupleIterator leftTuples, ITupleIterator rightTuples, List<Tuple> result) {
        List<Tuple> materializedLeft = materialize(leftTuples);
        List<Tuple> materializedRight = materialize(rightTuples);
        if (materializedLeft.size() <= materializedRight.size()) {
            intersect(materializedLeft, materializedRight, result);
        } else {
            intersect(materializedRight, materializedLeft, result);
        }
        AlgebraUtility.removeDuplicates(result);
    }

    @Override
    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.children.get(0).getAttributes(source, target);
    }

    @Override
    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitIntersection(this);
    }

    private List<Tuple> materialize(ITupleIterator tupleIterator) {
        List<Tuple> materialized = new ArrayList<>();
        while (tupleIterator.hasNext()) {
            materialized.add(tupleIterator.next());
        }
        tupleIterator.reset();
        return materialized;
    }

    private void intersect(List<Tuple> right, List<Tuple> left, List<Tuple> result) {
        Map<String, Tuple> cachedTuples = new HashMap<>();
        for (Tuple tuple : right) {
            cachedTuples.put(tuple.toStringNoOID(), tuple);
        }
        for (Tuple tuple : left) {
            Tuple tupleInCache = cachedTuples.get(tuple.toStringNoOID());
            if (tupleInCache != null) {
                result.add(tupleInCache);
            }
        }
    }
    
}
