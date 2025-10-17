package queryexecutor.model.algebra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.algebra.operators.ListTupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Tuple;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.utility.comparator.TupleComparatorOIDs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static queryexecutor.utility.QueryExecutorUtility.getCellValueForSorting;

public class OrderBy extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(OrderBy.class);

    public static final String ORDER_ASC = "ASC";
    public static final String ORDER_DESC = "DESC";

    private List<AttributeRef> attributes;
    private String order = ORDER_ASC;

    public OrderBy(List<AttributeRef> attributes) {
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Unable to create an OrderBy without attributes");
        }
        this.attributes = attributes;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getName() {
        return "ORDER BY-" + attributes;
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitOrderBy(this);
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        List<Tuple> result = new ArrayList<Tuple>();
        ITupleIterator originalTuples = children.get(0).execute(source, target);
        materializeResult(originalTuples, result, target);
        if (logger.isDebugEnabled()) logger.debug("Executing OrderBy: " + getName() + " on source\n" + (source == null ? "" : source.printInstances()) + "\nand target:\n" + target.printInstances());
        if (logger.isDebugEnabled()) logger.debug(getName() + " - Result: \n" + QueryExecutorUtility.printCollection(result));
        originalTuples.close();
        return new ListTupleIterator(result);
    }

    private void materializeResult(ITupleIterator originalTuples, List<Tuple> result, IDatabase target) {
        while (originalTuples.hasNext()) {
            Tuple originalTuple = originalTuples.next();
            result.add(originalTuple);
        }
        logger.trace("Unordered tuples {}", result);
        Collections.sort(result, new TupleOrderByComparator(attributes, target));
        logger.trace("Ordered tuples {}", result);
        if (ORDER_DESC.equals(order)) {
            Collections.reverse(result);
        }
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.attributes;
    }
}

class TupleOrderByComparator implements Comparator<Tuple> {

    private TupleComparatorOIDs tupleOIDComparator = new TupleComparatorOIDs();
    private List<AttributeRef> attributes;
    private IDatabase db;

    public TupleOrderByComparator(List<AttributeRef> attributes, IDatabase db) {
        this.attributes = attributes;
        this.db = db;
    }

    public int compare(Tuple t1, Tuple t2) {
        String s1 = buildTupleString(t1);
        String s2 = buildTupleString(t2);
        if (s1.equals(s2)) {
            return tupleOIDComparator.compare(t1, t2);
        }
        return s1.compareTo(s2);
    }

    private String buildTupleString(Tuple tuple) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (AttributeRef attribute : attributes) {
            Cell cell = findCell(attribute, tuple);
            result.append(getCellValueForSorting(db, cell)).append("|");
        }
        result.append("]");
        return result.toString();
    }

    private Cell findCell(AttributeRef attribute, Tuple tuple) {
        for (Cell cell : tuple.getCells()) {
//            if (DependencyUtility.unAlias(cell.getAttributeRef()).equals(attribute)) {
            if (QueryExecutorUtility.unAlias(cell.getAttributeRef()).equals(QueryExecutorUtility.unAlias(attribute))) {
                return cell;
            }
        }
        throw new IllegalArgumentException("Unable to find alias for attribute " + attribute + " in tuple " + tuple);
    }
}
