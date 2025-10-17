package queryexecutor.model.algebra.aggregatefunctions;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.*;

import java.util.Collections;
import java.util.List;
import queryexecutor.utility.comparator.TupleComparatorAttributeValue;

public class MinAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public MinAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public MinAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(QueryExecutorConstants.NULL_VALUE);
        }
        Collections.sort(tuples, new TupleComparatorAttributeValue(db, attributeRef));
        Collections.reverse(tuples);
        return tuples.get(0).getCell(attributeRef).getValue();
    }

    public String getName() {
        return "min";
    }

    public String toString() {
        return "min(" + attributeRef + ") as " + attributeRef.getName();
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public void setAttributeRef(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
    }
    public AttributeRef getNewAttributeRef() {
        return newAttributeRef;
    }

    public void setNewAttributeRef(AttributeRef newAttributeRef) {
        this.newAttributeRef = newAttributeRef;
    }

    public MinAggregateFunction clone() {
        try {
            MinAggregateFunction clone = (MinAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }

}
