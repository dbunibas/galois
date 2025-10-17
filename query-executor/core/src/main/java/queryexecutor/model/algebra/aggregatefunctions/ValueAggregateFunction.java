package queryexecutor.model.algebra.aggregatefunctions;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.AlgebraException;
import queryexecutor.model.database.*;

import java.util.List;

public class ValueAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public ValueAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public ValueAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(QueryExecutorConstants.NULL_VALUE);
        }
        if (!checkValues(tuples, attributeRef)) {
            throw new AlgebraException("Trying to extract aggregate value " + attributeRef + " from tuples with different values " + tuples);
        }
        return tuples.get(0).getCell(attributeRef).getValue();
    }

    private boolean checkValues(List<Tuple> tuples, AttributeRef attribute) {
        IValue first = tuples.get(0).getCell(attribute).getValue();
        for (Tuple tuple : tuples) {
            IValue value = tuple.getCell(attribute).getValue();
            if (!value.equals(first)) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return "value";
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public String toString() {
        return attributeRef.toString();
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

    public ValueAggregateFunction clone() {
        try {
            ValueAggregateFunction clone = (ValueAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }
}
