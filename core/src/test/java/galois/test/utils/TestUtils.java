package galois.test.utils;

import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestUtils {
    public static List<Tuple> loadTuplesFromCSV(String fileName, boolean header) {
        String csv = Objects.requireNonNull(TestUtils.class.getResource(fileName)).getFile();
        DAOMainMemoryDatabase dao = new DAOMainMemoryDatabase();
        IDatabase mainMemoryDB = dao.loadCSVDatabase(csv, ',', null, false, header);
        return toTupleList(mainMemoryDB.getFirstTable().getTupleIterator());
    }
    
    public static Stream<Tuple> toTupleStream(ITupleIterator iterator) {
        Iterable<Tuple> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static List<Tuple> toTupleList(ITupleIterator iterator) {
        return toTupleStream(iterator).toList();
    }
}
