package galois.test;

import galois.test.experiments.metrics.TupleCellSimilarityFilteredAttributes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.ConstantValue;
import speedy.model.database.IValue;
import speedy.model.database.Tuple;
import speedy.model.database.TupleOID;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The tuples used here either match exactly or differ on a numerical attribute: no LLM call is needed
@Slf4j
public class TestTupleCellSimilarityRank {

    private static final String SQL_NO_ORDER_BY = "select name, number from presidents";
    private static final String SQL_ORDER_BY = "select name, number from presidents order by number";

    @Test
    public void testSameOrderWithOrderBy() {
        TupleCellSimilarityFilteredAttributes metric = new TupleCellSimilarityFilteredAttributes();
        metric.setQuerySql(SQL_ORDER_BY);
        assertEquals(1.0, metric.getScore(null, presidents(), presidents()));
    }

    @Test
    public void testWrongOrderWithOrderBy() {
        TupleCellSimilarityFilteredAttributes metric = new TupleCellSimilarityFilteredAttributes();
        metric.setQuerySql(SQL_ORDER_BY);
        // only the tuple in the middle keeps its position: tp = 1, precision = recall = 1/3
        assertEquals(1.0 / 3, metric.getScore(null, presidents(), reversed(presidents())), 0.0001);
    }

    @Test
    public void testWrongOrderWithoutOrderBy() {
        TupleCellSimilarityFilteredAttributes metric = new TupleCellSimilarityFilteredAttributes();
        metric.setQuerySql(SQL_NO_ORDER_BY);
        assertEquals(1.0, metric.getScore(null, presidents(), reversed(presidents())));
    }

    @Test
    public void testWrongOrderWithoutSql() {
        TupleCellSimilarityFilteredAttributes metric = new TupleCellSimilarityFilteredAttributes();
        assertEquals(1.0, metric.getScore(null, presidents(), reversed(presidents())));
    }

    private List<Tuple> presidents() {
        return Arrays.asList(
                generateTuple(1, Arrays.asList("Washington", 1)),
                generateTuple(2, Arrays.asList("Adams", 2)),
                generateTuple(3, Arrays.asList("Jefferson", 3)));
    }

    private List<Tuple> reversed(List<Tuple> tuples) {
        List<Tuple> reversed = new java.util.ArrayList<>(tuples);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private Tuple generateTuple(int oidValue, List<Object> values) {
        List<String> attributeNames = Arrays.asList("name", "number");
        TupleOID oid = new TupleOID(oidValue);
        Tuple tuple = new Tuple(oid);
        for (int i = 0; i < attributeNames.size(); i++) {
            AttributeRef attributeRef = new AttributeRef("presidents", attributeNames.get(i));
            IValue value = new ConstantValue(values.get(i));
            tuple.addCell(new Cell(oid, attributeRef, value));
        }
        return tuple;
    }
}
