package galois.test;

import galois.test.experiments.metrics.LLMDistance;
import galois.test.experiments.metrics.TupleLLMSimilarityConstraintFilteredAttributes;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.ConstantValue;
import speedy.model.database.IValue;
import speedy.model.database.Tuple;
import speedy.model.database.TupleOID;

public class TestLLMDistance {

    @Test
    public void testDistance() {
        LLMDistance distance = new LLMDistance();
        String cell1 = "attribute=nobel_prize, value=chemistry";
        String cell2 = "attribute=nobel_prize, value=noble prize in chemistry";
        boolean areSimilar = distance.areCellSimilar(cell1, cell2, null);
        System.out.println(cell1 + " ---" + cell2 + ": " + areSimilar);
        areSimilar = distance.areCellSimilar(cell2, cell1, null);
        System.out.println(cell1 + " ---" + cell2 + ": " + areSimilar);
        String cell3 = "attribute=nobel_prize, value=noble prize in peace";
        String cell4 = "attribute=nobel_prize, value=peace";
        System.out.println(cell1 + " ---" + cell3 + ": " + distance.areCellSimilar(cell1, cell3, null));
        System.out.println(cell1 + " ---" + cell4 + ": " + distance.areCellSimilar(cell1, cell4, null));
        System.out.println(cell2 + " ---" + cell3 + ": " + distance.areCellSimilar(cell2, cell3, null));
        System.out.println(cell2 + " ---" + cell4 + ": " + distance.areCellSimilar(cell2, cell4, null));
    }
    
    @Test
    public void testTupleDistance() {
        LLMDistance distance = new LLMDistance();
        String tuple1 = "name: Mario, surname: Rossi, age: 30, ssn: 245";
        String tuple2 = "name: M., surname: Rossi, age: null, ssn: 245";
        System.out.println(tuple1 + " --- " + tuple2 + " --> " + distance.areTupleSimilar(tuple1, tuple2));
        System.out.println(tuple1 + " --- " + tuple2 + " --> " + distance.areTupleSimilar(tuple2, tuple1));
        String tuple3 = "name: N., surname: Rossi, age: null, ssn: 111";
        System.out.println(tuple1 + " --- " + tuple3 + " --> " + distance.areTupleSimilar(tuple1, tuple3));
        System.out.println(tuple2 + " --- " + tuple3 + " --> " + distance.areTupleSimilar(tuple2, tuple3));
    }
    
    @Test
    public void testTupleLLMSimilarity() {
        Tuple t1 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Mario", "Rossi", 30, 245));
        Tuple t2 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("M.", "Rossi", 30, 245));
        Tuple t3 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("N.", "Verdi", 20, 111));
        Tuple t4 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Nicola", "Verdi", 20, 111));
        Tuple t5 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Alberto", "Angela", 50, 6678));
        Tuple t6 = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Mario", "Rossi", 30, 245));
        
        List<Tuple> expected = Arrays.asList(t4, t1, t5);
        List<Tuple> actual = Arrays.asList(t2, t3, t6);
        
        TupleLLMSimilarityConstraintFilteredAttributes metric = new TupleLLMSimilarityConstraintFilteredAttributes();
        Double score = metric.getScore(null, expected, actual);
        System.out.println(score);
    }
    
    private Tuple generateTuple(int oidValue, String tableName, List<String> attributeNames, List<Object> values) {
        TupleOID oid = new TupleOID(oidValue);
        Tuple t = new Tuple(oid);
        for(int i = 0; i < attributeNames.size(); i++) {
            AttributeRef a = new AttributeRef(tableName, attributeNames.get(i));
            IValue value = new ConstantValue(values.get(i));
            t.addCell(new Cell(oid, a, value));
        }
        return t;
    }

}
