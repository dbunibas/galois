package queryexecutor.model.algebra.aggregatefunctions;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.*;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.utility.comparator.TupleComparatorAttributeValue;

public class MaxAggregateFunction implements IAggregateFunction {

    private static Logger logger = LoggerFactory.getLogger(MaxAggregateFunction.class);

    private AttributeRef attributeRef;
    private AttributeRef newAttributeRef;

    public MaxAggregateFunction(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = attributeRef;
    }

    public MaxAggregateFunction(AttributeRef attributeRef, AttributeRef newAttributeRef) {
        this.attributeRef = attributeRef;
        this.newAttributeRef = newAttributeRef;
    }

    public IValue evaluate(IDatabase db, List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new NullValue(QueryExecutorConstants.NULL_VALUE);
        }
        if (logger.isDebugEnabled()) logger.debug("Computing max in " + QueryExecutorUtility.printCollection(tuples));
        Collections.sort(tuples, new TupleComparatorAttributeValue(db, attributeRef));
        return tuples.get(0).getCell(attributeRef).getValue();
    }

    public String getName() {
        return "max";
    }

    public String toString() {
        return "max(" + attributeRef + ") as " + attributeRef.getName();
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

    public MaxAggregateFunction clone() {
        try {
            MaxAggregateFunction clone = (MaxAggregateFunction) super.clone();
            clone.attributeRef = this.attributeRef.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException("Unable to clone " + ex.getLocalizedMessage());
        }
    }

}
