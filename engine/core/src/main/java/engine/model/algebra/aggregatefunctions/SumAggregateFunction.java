package engine.model.algebra.aggregatefunctions;

import engine.EngineConstants;
import engine.model.database.*;

import java.util.List;

public class SumAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public SumAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public SumAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(EngineConstants.NULL_VALUE);
        }
        double sum = 0;
        for (Tuple tuple : tuples) {
            IValue value = tuple.getCell(attributeRef).getValue();
            if (tuple.getCell(attributeRef).getValue() instanceof NullValue) continue;
            try {
                double doubleValue = Double.parseDouble(value.toString());
                sum += doubleValue;
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unable to compute sum on non-numerical value " + value);
            }
        }
        return new ConstantValue(sum + "");
    }

    public String getName() {
        return "sum";
    }

    public String toString() {
        return "sum(" + attributeRef + ") as " + attributeRef.getName();
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

    public SumAggregateFunction clone() {
        try {
            SumAggregateFunction clone = (SumAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }
}
