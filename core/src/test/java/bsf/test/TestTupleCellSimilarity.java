package bsf.test;

import bsf.test.experiments.metrics.EditDistance;
import bsf.test.experiments.metrics.TupleCellSimilarityFilteredAttributes;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.ConstantValue;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.TupleOID;

public class TestTupleCellSimilarity {
    
    @Test
    public void testIdenticalTuples() {
        Tuple t1_actual = generateTuple(1, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Mario", "Rossi", 30, 245));
        Tuple t1_expected = generateTuple(2, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Mario", "Rossi", 30, 245));
        Tuple t2_actual = generateTuple(3, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Nicola", "Verdi", 20, 111));
        Tuple t2_expected = generateTuple(4, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Nicola", "Verdi", 20, 111));
        Tuple t3_actual = generateTuple(5, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Alberto", "Angela", 50, 6678));
        Tuple t3_excpected = generateTuple(6, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Alberto", "Rossi", 49, 245));
        Tuple t4_excpected = generateTuple(7, "people", Arrays.asList("name", "surname", "age", "ssn"), Arrays.asList("Alberto", "Rossi", 49, 6677));
        
        List<Tuple> actual = Arrays.asList(t1_actual, t2_actual, t3_actual);
        List<Tuple> expected = Arrays.asList(t1_expected, t2_expected, t3_excpected, t4_excpected);
        
        TupleCellSimilarityFilteredAttributes similarity = new TupleCellSimilarityFilteredAttributes();
        Double score = similarity.getScore(null, expected, actual);
        System.out.println("Score: " + score);
    }
    
    @Test
    public void testSimilarity() {
        Tuple t1_actual = generateTuple(1, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Donald J. Trumph", "republican", "USA", 47));
        Tuple t2_actual = generateTuple(2, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("B. Clinton", "democratic", "USA", 40));
        Tuple t3_actual = generateTuple(3, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Donald J. Trumph", "republican", "USA", 45));
        Tuple t4_actual = generateTuple(4, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Donald J. Trumph", "democratico", "Italy", 47));
        Tuple t1_expected = generateTuple(5, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Trumph J. Donald", "republican", "United States", 47));
        Tuple t2_expected = generateTuple(6, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Bill Clinton", "democratic", "United States of America", 41));
        Tuple t3_expected = generateTuple(7, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Mattarella Sergio", "democratico", "Italy", 47));

        List<Tuple> actual = Arrays.asList(t1_actual, t2_actual, t3_actual, t4_actual);
        List<Tuple> expected = Arrays.asList(t1_expected, t2_expected, t3_expected);
        
        TupleCellSimilarityFilteredAttributes similarity = new TupleCellSimilarityFilteredAttributes();
        Double score = similarity.getScore(null, expected, actual);
        System.out.println("Score: " + score);
    }
    
    @Test
    public void testLevenstein() {
        EditDistance distance = new EditDistance();
        Tuple t1 = generateTuple(1, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("Donald J. Trumph", "republican", "USA", 47));
        Tuple t2 = generateTuple(2, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("B. Clinton", "democratic", "USA", 40));
        Tuple t3 = generateTuple(2, "presidents", Arrays.asList("name", "party", "country", "number"), Arrays.asList("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "yyyyyyyyyyyhdajahdjkhkjashdkjahashdkhakjhakjhdyyyyyyyyyyyyyyyyyyyyy", "USA", 40));
        double score = distance.getScoreForTuple(t1, t3);
        System.out.println("Score: " + score);
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
