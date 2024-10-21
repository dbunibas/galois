package engine.model.algebra.aggregatefunctions;

import engine.model.database.*;

import java.util.List;

public class CountAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public CountAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public CountAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        return new ConstantValue(tuples.size());
    }

    public String getName() {
        return "count";
    }

    public String toString() {
        return "count(" + attributeRef + ")";
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

    public CountAggregateFunction clone() {
        try {
            CountAggregateFunction clone = (CountAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }
}
