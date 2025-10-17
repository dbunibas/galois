package queryexecutor.model.algebra;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.Tuple;

public class SelectNotIn extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(SelectNotIn.class);

    private List<AttributeRef> attributes;
    private IAlgebraOperator selectionOperator;

    public SelectNotIn(List<AttributeRef> attributes, IAlgebraOperator selectionOperator) {
        assert (selectionOperator != null);
        this.attributes = attributes;
        this.selectionOperator = selectionOperator;
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitSelectNotIn(this);
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT").append(attributes).append(" NOT IN (\n");
        sb.append(selectionOperator.toString(QueryExecutorConstants.INDENT + QueryExecutorConstants.INDENT)).append("\n");
        sb.append(")");
        return sb.toString();
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        if (attributes.size() != selectionOperator.getAttributes(source, target).size()) {
            throw new IllegalArgumentException("Attribute sizes are different: " + attributes + " - " + selectionOperator.getAttributes(source, target));
        }
        List<List<String>> valueMap = materializeInnerOperator(source, target);
        SelectNotInTupleIterator tupleIterator = new SelectNotInTupleIterator(children.get(0).execute(source, target), valueMap);
        if (logger.isDebugEnabled()) logger.debug("Executing SelectIn: " + getName() + " in attributes\n" + attributes + "Map:\n" + QueryExecutorUtility.printCollection(valueMap) + " on source\n" + (source == null ? "" : source.printInstances()) + "\nand target:\n" + target.printInstances());
        if (logger.isDebugEnabled()) logger.debug("Result: " + QueryExecutorUtility.printTupleIterator(tupleIterator));
        if (logger.isDebugEnabled()) tupleIterator.reset();
        return tupleIterator;
    }

    private List<List<String>> materializeInnerOperator(IDatabase source, IDatabase target) {
        List<List<String>> result = new ArrayList<List<String>>();
        List<String> tuplesForOperator = new ArrayList<String>();
        result.add(tuplesForOperator);
        ITupleIterator tuples = selectionOperator.execute(source, target);
        while (tuples.hasNext()) {
            Tuple tuple = tuples.next();
            tuplesForOperator.add(buildTupleSignature(tuple, selectionOperator.getAttributes(source, target)));
        }
        return result;
    }

    private String buildTupleSignature(Tuple tuple, List<AttributeRef> attributes) {
        StringBuilder stringForTuple = new StringBuilder();
        for (AttributeRef attribute : attributes) {
            IValue value = tuple.getCell(attribute).getValue();
            stringForTuple.append(value.toString()).append("-");
        }
        return stringForTuple.toString();
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return attributes;
    }

    public IAlgebraOperator getSelectionOperator() {
        return selectionOperator;
    }

    @Override
    public IAlgebraOperator clone() {
        SelectNotIn clone = (SelectNotIn) super.clone();
        clone.selectionOperator = selectionOperator.clone();
        return clone;
    }

    class SelectNotInTupleIterator implements ITupleIterator {

        private ITupleIterator tableIterator;
        private Tuple nextTuple;
        private List<List<String>> innerTuples;

        public SelectNotInTupleIterator(ITupleIterator tableIterator, List<List<String>> innerTuples) {
            this.innerTuples = innerTuples;
            this.tableIterator = tableIterator;
        }

        public boolean hasNext() {
            if (nextTuple != null) {
                return true;
            } else {
                loadNextTuple();
                return nextTuple != null;
            }
        }

        private void loadNextTuple() {
            while (tableIterator.hasNext()) {
                Tuple tuple = tableIterator.next();
                if (conditionsAreTrue(tuple)) {
                    nextTuple = tuple;
                    return;
                }
            }
            nextTuple = null;
        }

        private boolean conditionsAreTrue(Tuple tuple) {
            String tupleSignature = buildTupleSignature(tuple, attributes);
            for (List<String> tuplesForInternalSelector : innerTuples) {
                if (tuplesForInternalSelector.contains(tupleSignature)) {
                    if (logger.isDebugEnabled()) logger.debug("Inner tuples doesn't contain tuple " + tupleSignature + "\n Inner tuples: " + QueryExecutorUtility.printCollection(innerTuples));
                    return false;
                }
            }
            return true;
        }

        public Tuple next() {
            if (nextTuple != null) {
                Tuple result = nextTuple;
                nextTuple = null;
                return result;
            }
            return null;
        }

        public void reset() {
            this.tableIterator.reset();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

        public void close() {
            tableIterator.close();
        }

    }
}
