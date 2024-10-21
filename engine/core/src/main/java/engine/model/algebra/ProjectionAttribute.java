package engine.model.algebra;

import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.model.database.AttributeRef;

public class ProjectionAttribute implements Cloneable {

    private AttributeRef attributeRef;
    private IAggregateFunction aggregateFunction;

    public ProjectionAttribute(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
    }

    public ProjectionAttribute(IAggregateFunction aggregateFunction) {
        this.aggregateFunction = aggregateFunction;
    }

    public AttributeRef getAttributeRef() {
        if (isAggregative()) {
            return aggregateFunction.getNewAttributeRef();
        } else {
            return attributeRef;
        }
    }

    public IAggregateFunction getAggregateFunction() {
        return aggregateFunction;
    }

    @Override
    public String toString() {
        if (isAggregative()) {
            return aggregateFunction.getNewAttributeRef().toString();
        } else {
            return attributeRef.toString();
        }
    }

    public boolean isAggregative() {
        return aggregateFunction != null;
    }

    public ProjectionAttribute clone() {
        try {
            ProjectionAttribute clone = (ProjectionAttribute) super.clone();
            if (this.attributeRef != null) clone.attributeRef = this.attributeRef.clone();
            if (this.aggregateFunction != null) clone.aggregateFunction = this.aggregateFunction.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone ProjectionAttribute");
        }
    }
}
