package engine.model.algebra;

import engine.EngineConstants;
import engine.utility.EngineUtility;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.Tuple;

public class SelectIn extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(SelectIn.class);

    private List<AttributeRef> attributes;
    private List<IAlgebraOperator> selectionOperators;

    public SelectIn(List<AttributeRef> attributes, List<IAlgebraOperator> selectionOperators) {
        assert (!selectionOperators.isEmpty());
        this.attributes = attributes;
        this.selectionOperators = selectionOperators;
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitSelectIn(this);
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT").append(attributes).append(" IN (\n");
        for (IAlgebraOperator selectionOperator : selectionOperators) {
            sb.append(selectionOperator.toString(EngineConstants.INDENT + EngineConstants.INDENT)).append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        for (IAlgebraOperator selectionOperator : selectionOperators) {
            if (attributes.size() != selectionOperator.getAttributes(source, target).size()) {
                throw new IllegalArgumentException("Attribute sizes are different: " + attributes + " - " + selectionOperator.getAttributes(source, target));
            }
        }
        List<List<String>> valueMap = materializeInnerOperator(source, target);
        SelectInTupleIterator tupleIterator = new SelectInTupleIterator(children.get(0).execute(source, target), valueMap);
        if (logger.isDebugEnabled()) logger.debug("Executing SelectIn: " + getName() + " in attributes\n" + attributes + "Map:\n" + EngineUtility.printCollection(valueMap) + " on source\n" + (source == null ? "" : source.printInstances()) + "\nand target:\n" + target.printInstances());
        if (logger.isDebugEnabled()) logger.debug("Result: " + EngineUtility.printTupleIterator(tupleIterator));
        if (logger.isDebugEnabled()) tupleIterator.reset();
        return tupleIterator;
    }

    private List<List<String>> materializeInnerOperator(IDatabase source, IDatabase target) {
        List<List<String>> result = new ArrayList<List<String>>();
        for (IAlgebraOperator selectionOperator : selectionOperators) {
            List<String> tuplesForOperator = new ArrayList<String>();
            result.add(tuplesForOperator);
            ITupleIterator tuples = selectionOperator.execute(source, target);
            while (tuples.hasNext()) {
                Tuple tuple = tuples.next();
                tuplesForOperator.add(buildTupleSignature(tuple, selectionOperator.getAttributes(source, target)));
            }
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

    public List<IAlgebraOperator> getSelectionOperators() {
        return selectionOperators;
    }

    @Override
    public IAlgebraOperator clone() {
        SelectIn clone = (SelectIn) super.clone();
        clone.selectionOperators = new ArrayList<IAlgebraOperator>();
        for (IAlgebraOperator selectionOperator : selectionOperators) {
            clone.selectionOperators.add((Scan) selectionOperator.clone());
        }
        return clone;
    }

    class SelectInTupleIterator implements ITupleIterator {

        private ITupleIterator tableIterator;
        private Tuple nextTuple;
        private List<List<String>> innerTuples;

        public SelectInTupleIterator(ITupleIterator tableIterator, List<List<String>> innerTuples) {
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
                if (!tuplesForInternalSelector.contains(tupleSignature)) {
                    if (logger.isDebugEnabled()) logger.debug("Inner tuples doesn't contain tuple " + tupleSignature + "\n Inner tuples: " + EngineUtility.printCollection(innerTuples));
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