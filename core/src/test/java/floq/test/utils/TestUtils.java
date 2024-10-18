package floq.test.utils;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import engine.exceptions.DAOException;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.IDatabase;
import engine.model.database.Tuple;
import engine.persistence.DAOMainMemoryDatabase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestUtils {
    public static Document buildDOMFromString(String content) throws DAOException {
        if (content == null || content.isEmpty()) {
            throw new DAOException("Unable to load xml from empty content.");
        }
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING); // builder.setValidation(false); is deprecated
        Document document = null;
        try {
            document = builder.build(new ByteArrayInputStream(content.getBytes()));
            return document;
        } catch (JDOMException | IOException ex) {
            throw new DAOException(ex.getMessage());
        }
    }

    public static List<Tuple> loadTuplesFromCSV(String fileName, boolean header) {
        String csv = Objects.requireNonNull(TestUtils.class.getResource(fileName)).getFile();
        DAOMainMemoryDatabase dao = new DAOMainMemoryDatabase();
        IDatabase mainMemoryDB = dao.loadCSVDatabase(csv, ',', null, false, header);
        // TODO: getFirstTable() is inadequate if loading a full database (for query execution)
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
