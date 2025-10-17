package queryexecutor.model.algebra.aggregatefunctions;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.*;

import java.util.List;

public class AvgAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public AvgAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public AvgAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(QueryExecutorConstants.NULL_VALUE);
        }
        double sum = 0;
        long count = 0;
        for (Tuple tuple : tuples) {
            IValue value = tuple.getCell(attributeRef).getValue();
            if (tuple.getCell(attributeRef).getValue() instanceof NullValue) continue;
            try {
                double doubleValue = Double.parseDouble(value.toString());
                sum += doubleValue;
                count++;
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unable to compute average on non-numerical value " + value);
            }
        }
        double avg = sum / (double) count;
        return new ConstantValue(avg + "");
    }

    public String getName() {
        return "avg";
    }

    public String toString() {
        return "avg(" + attributeRef + ") as " + attributeRef.getName();
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

    public AvgAggregateFunction clone() {
        try {
            AvgAggregateFunction clone = (AvgAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }

}
