package queryexecutor.utility;

import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.algebra.operators.GenerateTupleFromTuplePair;
import queryexecutor.persistence.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.nfunk.jep.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.Tuple;
import queryexecutor.utility.comparator.StringComparator;
import queryexecutor.utility.comparator.TupleComparatorWithoutOIDs;

@SuppressWarnings("unchecked")
public class AlgebraUtility {

    private static Logger logger = LoggerFactory.getLogger(AlgebraUtility.class);
    private static GenerateTupleFromTuplePair tupleMerger = new GenerateTupleFromTuplePair();

    public static void addIfNotContained(List list, Object object) {
        QueryExecutorUtility.addIfNotContained(list, object);
    }

    public static IValue getCellValue(Tuple tuple, AttributeRef attributeRef) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttributeRef().equals(attributeRef)) {
                return cell.getValue();
            }
        }
        throw new IllegalArgumentException("Unable to find attribute " + attributeRef + " in tuple " + tuple.toStringWithOIDAndAlias());
    }

    public static boolean contains(Tuple tuple, AttributeRef attributeRef) {
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttributeRef().equals(attributeRef)) {
                return true;
            }
        }
        return false;
    }

    public static List<Object> getTupleValuesExceptOIDs(Tuple tuple) {
        List<Object> values = new ArrayList<Object>();
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equals(QueryExecutorConstants.OID)) {
                continue;
            }
            IValue attributeValue = cell.getValue();
            values.add(attributeValue.getPrimitiveValue().toString());
        }
        return values;
    }

    public static List<Object> getNonOidTupleValues(Tuple tuple) {
        List<Object> values = new ArrayList<Object>();
        for (Cell cell : tuple.getCells()) {
            if (cell.getAttribute().equals(QueryExecutorConstants.OID)) {
                continue;
            }
            IValue attributeValue = cell.getValue();
            values.add(attributeValue.getPrimitiveValue());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public static boolean equalLists(List list1, List list2) {
        return (list1.containsAll(list2) && list2.containsAll(list1));
    }

    public static boolean areEqualExcludingOIDs(Tuple t1, Tuple t2) {
        if (t1 == null || t2 == null) {
            return false;
        }
        return equalLists(getTupleValuesExceptOIDs(t1), getTupleValuesExceptOIDs(t2));
    }

    public static void removeDuplicates(List result) {
        if (result.isEmpty()) {
            return;
        }
        Collections.sort(result, new StringComparator());
        Iterator tupleIterator = result.iterator();
        String prevValues = tupleIterator.next().toString();
        while (tupleIterator.hasNext()) {
            Object currentTuple = tupleIterator.next();
            String currentValues = currentTuple.toString();
            if (prevValues.equals(currentValues)) {
                tupleIterator.remove();
            } else {
                prevValues = currentValues;
            }
        }
    }
    
    public static void removeTupleDuplicatesIgnoreOID(List<Tuple> result) {
        if (result.isEmpty()) {
            return;
        }
        if (logger.isDebugEnabled()) logger.debug("Tuples: {}", result);
        Collections.sort(result, new TupleComparatorWithoutOIDs());
        Iterator<Tuple> tupleIterator = result.iterator();
        String prevValues = tupleIterator.next().toStringNoOID();
        while (tupleIterator.hasNext()) {
            Tuple currentTuple = tupleIterator.next();
            String currentValues = currentTuple.toStringNoOID();
            if (prevValues.equals(currentValues)) {
                tupleIterator.remove();
            } else {
                prevValues = currentValues;
            }
        }
    }

    public static boolean isPlaceholder(Variable jepVariable) {
        return jepVariable.getDescription().toString().startsWith("$$");
//        return jepVariable.getDescription().toString().startsWith("$$") &&
//                jepVariable.getDescription().toString().endsWith("#");
    }

    private static Comparable getTypedValue(IValue value, String type) {
        if (type.equals(Types.LONG) || type.equals(Types.REAL) || type.equals(Types.INTEGER)) {
            return Double.parseDouble(value.toString());
        }
        return value.toString();
    }

    private static boolean areNotCompatible(String firstType, String secondType) {
        return !firstType.equals(secondType);
    }

}
