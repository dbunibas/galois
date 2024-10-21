package engine.model.algebra;

import engine.EngineConstants;
import engine.model.algebra.operators.EvaluateExpression;
import engine.model.database.*;
import engine.model.expressions.ExpressionAttributeRef;
import engine.utility.EngineUtility;
import engine.model.algebra.operators.ListTupleIterator;
import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.aggregatefunctions.IAggregateFunction;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;

public class Project extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(Project.class);

    private static EvaluateExpression expressionEvaluator = new EvaluateExpression();
    private List<ProjectionAttribute> attributes;
    private List<AttributeRef> newAttributes;
    private boolean discardOids;

    public Project(List<ProjectionAttribute> attributes) {
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Unable to create a Project with no attributes");
        }
        if (!areCompatible(attributes)) {
            throw new IllegalArgumentException("Unable to mix aggregative and non aggregative attributes " + attributes);
        }
        this.attributes = attributes;
    }

    public Project(List<ProjectionAttribute> attributes, List<AttributeRef> newAttributes, boolean discardOids) {
        this(attributes);
        this.newAttributes = newAttributes;
        this.discardOids = discardOids;
    }

    public String getName() {
        return "PROJECT-" + attributes + (newAttributes != null ? " as " + newAttributes : "");
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitProject(this);
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        List<Tuple> result = new ArrayList<Tuple>();
        ITupleIterator originalTuples = children.get(0).execute(source, target);
        materializeResult(originalTuples, result);
        if (logger.isDebugEnabled()) logger.debug(getName() + " - Result: \n" + EngineUtility.printCollection(result));
        originalTuples.close();
        if (isAggregative()) {
            result = aggregateResult(target, result);
        }
        checkResult(result);
        return new ListTupleIterator(result);
    }

    private void materializeResult(ITupleIterator originalTuples, List<Tuple> result) {
        while (originalTuples.hasNext()) {
            Tuple originalTuple = originalTuples.next();
            if (originalTuple == null) continue;
            if (logger.isDebugEnabled()) logger.debug("Original tuple: " + originalTuple.toStringWithOIDAndAlias());
            Tuple projectedTuple = projectTuple(originalTuple);
            if (logger.isDebugEnabled()) logger.debug("Projected tuple: " + projectedTuple.toStringWithOIDAndAlias());
            if (newAttributes != null && !isAggregative()) {
                projectedTuple = renameAttributes(projectedTuple);
            }
            result.add(projectedTuple);
        }
//        AlgebraUtility.removeDuplicates(result);
    }

    protected Tuple projectTuple(Tuple originalTuple) {
        if (isAggregative()) {
            return originalTuple;
        }
        Tuple tuple = originalTuple.clone();
        List<Cell> cells = tuple.getCells();
        for (Iterator<Cell> it = cells.iterator(); it.hasNext();) {
            Cell cell = it.next();
            if (cell.getAttribute().equals(EngineConstants.OID) && !discardOids) {
                TableAlias tableAlias = cell.getAttributeRef().getTableAlias();
                if (isToRemove(tableAlias)) {
                    it.remove();
                }
            } else if (!isToProject(cell.getAttributeRef(), this.attributes)) {
                it.remove();
            }
        }
        generateExpressionAttributes(tuple, originalTuple);
        sortTupleAttributes(tuple, this.attributes);
        return tuple;
    }

    private void generateExpressionAttributes(Tuple tuple, Tuple originalTuple) {
        for (ProjectionAttribute attribute : this.attributes) {
            if(!(attribute.getAttributeRef() instanceof ExpressionAttributeRef)){
                continue;
            }
            ExpressionAttributeRef expressionAttributeRef = (ExpressionAttributeRef) attribute.getAttributeRef();
            Object value = expressionEvaluator.evaluateConditionRaw(expressionAttributeRef.getExpression(), originalTuple);
            Cell cell = new Cell(tuple.getOid(), expressionAttributeRef, new ConstantValue(value));
            tuple.addCell(cell);
        }
    }

    protected void sortTupleAttributes(Tuple tuple, List<ProjectionAttribute> projectionAttributes) {
        List<Cell> sortedCells = new ArrayList<Cell>();
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equalsIgnoreCase(EngineConstants.OID)) {
                EngineUtility.addIfNotContained(sortedCells, cell);
            }
        }
        for (ProjectionAttribute projectionAttribute : projectionAttributes) {
            EngineUtility.addIfNotContained(sortedCells, tuple.getCell(projectionAttribute.getAttributeRef()));
        }
        if (tuple.getCells().size() != sortedCells.size()) {
            throw new IllegalArgumentException("Tuples after sorting have differents cells:\n Tuple cells:" + tuple.getCells() + "\n Sorted cells:" + sortedCells + "\n Projection Attributes: " + this.attributes);
        }
        tuple.setCells(sortedCells);
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        List<AttributeRef> result = new ArrayList<AttributeRef>();
        for (ProjectionAttribute projectionAttribute : this.attributes) {
            result.add(projectionAttribute.getAttributeRef());
        }
        return result;
    }

    public List<IAggregateFunction> getAggregateFunctions() {
        List<IAggregateFunction> result = new ArrayList<IAggregateFunction>();
        for (ProjectionAttribute projectionAttribute : this.attributes) {
            result.add(projectionAttribute.getAggregateFunction());
        }
        return result;
    }

    private List<Tuple> aggregateResult(IDatabase db, List<Tuple> tuplesToAggregate) {
        Tuple tuple = new Tuple(new TupleOID(IntegerOIDGenerator.getNextOID()));
        for (int i = 0; i < attributes.size(); i++) {
            ProjectionAttribute attribute = attributes.get(i);
            IAggregateFunction function = attribute.getAggregateFunction();
            AttributeRef newAttribute = function.getNewAttributeRef();
            if (newAttributes != null) {
                newAttribute = newAttributes.get(i);
            }
            IValue aggregateValue = function.evaluate(db, tuplesToAggregate);
            Cell cell = new Cell(tuple.getOid(), newAttribute, aggregateValue);
            tuple.addCell(cell);
        }
        List<Tuple> result = new ArrayList<Tuple>();
        result.add(tuple);
        return result;
    }

    public List<AttributeRef> getNewAttributes() {
        return newAttributes;
    }

    public boolean isAggregative() {
        for (ProjectionAttribute attribute : attributes) {
            if (attribute.isAggregative()) {
                return true;
            }
        }
        return false;
    }

    private boolean isToRemove(TableAlias tableAlias) {
        for (ProjectionAttribute attribute : attributes) {
            if (attribute.getAttributeRef().getTableAlias().equals(tableAlias)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isToProject(AttributeRef attributeRef, List<ProjectionAttribute> projectionAttributes) {
        for (ProjectionAttribute attribute : projectionAttributes) {
            if (attribute.getAttributeRef().equals(attributeRef)) {
                return true;
            }
        }
        return false;
    }

    private Tuple renameAttributes(Tuple projectedTuple) {
        int i = 0;
        for (Cell cell : projectedTuple.getCells()) {
            if (cell.isOID() && !discardOids) {
                continue;
            }
            cell.setAttributeRef(newAttributes.get(i));
            i++;
        }
        return projectedTuple;
    }

    private void checkResult(List<Tuple> result) {
        if (result.isEmpty()) {
            return;
        }
        Tuple firstTuple = result.get(0);
        if (newAttributes == null) {
            for (ProjectionAttribute projectionAttribute : attributes) {
                if (!containsAttribute(firstTuple, projectionAttribute.getAttributeRef())) {
                    throw new IllegalArgumentException("Missing attribute " + projectionAttribute + " after projection: " + firstTuple.getCells() + " - Expected attributes: " + attributes);
                }
            }
        } else {
            for (AttributeRef attribute : newAttributes) {
                if (!containsAttribute(firstTuple, attribute)) {
                    throw new IllegalArgumentException("Missing attribute " + attribute + " after projection: " + firstTuple.getCells() + " - Expected attributes: " + newAttributes);
                }
            }
        }
    }

    private boolean containsAttribute(Tuple tuple, AttributeRef attribute) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttributeRef().equals(attribute)) {
                return true;
            }
        }
        return false;
    }

    private boolean areCompatible(List<ProjectionAttribute> attributes) {
        boolean containsAggregative = false;
        boolean containsNonAggregative = false;
        for (ProjectionAttribute attribute : attributes) {
            if (attribute.isAggregative()) {
                containsAggregative = true;
            } else {
                containsNonAggregative = true;
            }
            if (containsAggregative && containsNonAggregative) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IAlgebraOperator clone() {
        Project clone = (Project) super.clone();
        if (attributes != null) {
            clone.attributes = new ArrayList<ProjectionAttribute>();
            for (ProjectionAttribute newAttribute : attributes) {
                clone.attributes.add(newAttribute.clone());
            }
        }
        if (newAttributes != null) {
            clone.newAttributes = new ArrayList<AttributeRef>();
            for (AttributeRef newAttribute : newAttributes) {
                clone.newAttributes.add(newAttribute.clone());
            }
        }
        return clone;
    }
}
