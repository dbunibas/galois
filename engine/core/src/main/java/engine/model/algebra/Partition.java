package engine.model.algebra;

import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.utility.EngineUtility;
import engine.model.algebra.operators.ListTupleIterator;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.AttributeRef;
import engine.model.database.Cell;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Partition extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(Partition.class);

    private List<AttributeRef> groupingAttributes;
    private List<IAggregateFunction> aggregateFunctions;

    public Partition(List<AttributeRef> groupingAttributes, List<IAggregateFunction> aggregateFunctions) {
        this.groupingAttributes = groupingAttributes;
        this.aggregateFunctions = aggregateFunctions;
    }

    public String getName() {
        return "PARTITION-" + groupingAttributes + " - SELECT " + aggregateFunctions;
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitPartition(this);
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        if (logger.isDebugEnabled()) logger.debug("Executing groupby: " + getName() + " on source\n" + (source == null ? "" : source.printInstances()) + "\nand target:\n" + target.printInstances());
        List<Tuple> result = new ArrayList<Tuple>();
        ITupleIterator originalTuples = children.get(0).execute(source, target);
        materializeResult(target, originalTuples, result);
        originalTuples.close();
        if (logger.isDebugEnabled()) logger.debug("Result:\n" + EngineUtility.printCollection(result));
        return new ListTupleIterator(result);
    }

    private void materializeResult(IDatabase db, ITupleIterator originalTuples, List<Tuple> result) {
        Map<String, List<Tuple>> groups = groupTuples(originalTuples);
        for (List<Tuple> group : groups.values()) {
            for (Tuple tuple : group) {
                for (IAggregateFunction function : aggregateFunctions) {
                    IValue aggregateValue = function.evaluate(db, group);
                    Cell cell = new Cell(tuple.getOid(), function.getAttributeRef(), aggregateValue);
                    tuple.addCell(cell);
                }
                result.add(tuple);
            }
        }
    }

    public static List<Object> getTupleValues(Tuple tuple, List<AttributeRef> attributes) {
        List<Object> values = new ArrayList<Object>();
        for (AttributeRef attribute : attributes) {
            values.add(tuple.getCell(attribute).getValue());
        }
        return values;
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.groupingAttributes;
    }

    private String generateKey(List<Object> tupleValues) {
        StringBuilder result = new StringBuilder("|");
        for (Object value : tupleValues) {
            result.append(value).append("|");
        }
        return result.toString();
    }

    private Map<String, List<Tuple>> groupTuples(ITupleIterator originalTuples) {
        Map<String, List<Tuple>> groups = new HashMap<String, List<Tuple>>();
        while (originalTuples.hasNext()) {
            Tuple originalTuple = originalTuples.next();
            List<Object> tupleValues = getTupleValues(originalTuple, groupingAttributes);
            String key = generateKey(tupleValues);
            List<Tuple> groupForKey = groups.get(key);
            if (groupForKey == null) {
                groupForKey = new ArrayList<Tuple>();
                groups.put(key, groupForKey);
            }
            groupForKey.add(originalTuple.clone());
        }
        return groups;
    }

    public List<AttributeRef> getGroupingAttributes() {
        return groupingAttributes;
    }

    public List<IAggregateFunction> getAggregateFunctions() {
        return aggregateFunctions;
    }
}