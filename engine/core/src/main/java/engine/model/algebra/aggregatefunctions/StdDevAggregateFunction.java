package engine.model.algebra.aggregatefunctions;

import engine.EngineConstants;
import engine.model.database.*;

import java.util.List;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class StdDevAggregateFunction implements IAggregateFunction {

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public StdDevAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public StdDevAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(EngineConstants.NULL_VALUE);
        }
        SummaryStatistics stats = new SummaryStatistics();
        for (Tuple tuple : tuples) {
            IValue value = tuple.getCell(attributeRef).getValue();
            if (tuple.getCell(attributeRef).getValue() instanceof NullValue) continue;
            try {
                double doubleValue = Double.parseDouble(value.toString());
                stats.addValue(doubleValue);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unable to compute average on non-numerical value " + value);
            }
        }
        return new ConstantValue(stats.getStandardDeviation());
    }

    public String getName() {
        return "stddev";
    }

    public String toString() {
        return "stddev(" + attributeRef + ") as " + attributeRef.getName();
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

    public StdDevAggregateFunction clone() {
        try {
            StdDevAggregateFunction clone = (StdDevAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }

}
