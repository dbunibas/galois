package com.galois.sqlparser.test;

import com.galois.sqlparser.SQLQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.SpeedyConstants;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.ITable;
import speedy.model.database.Tuple;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class TestDatabaseQuery {
    private static final String TABLE_NAME = "movie";

    private static DBMSDB db;

    @BeforeAll
    public static void setUp() {
        if (!SpeedyConstants.DBMS_DEBUG) {
            throw new UnsupportedOperationException("TestDatabaseQuery can be executed only if SpeedyConstants.DBMS_DEBUG is true!");
        }

        AccessConfiguration accessConfiguration = new AccessConfiguration();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:llm_directors_movies";
        String schemaName = "public";
        String username = "pguser";
        String password = "pguser";
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
        db = new DBMSDB(accessConfiguration);
    }

    @Test
    public void testSimpleSelectQuery() {
        String sql = String.format("select * from %s t limit 10", TABLE_NAME);
        ITable table = db.getTable(TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(10, tuples.size());
    }

    @Test
    public void testComplexQuery() {
        String sql = String.format("select m.startyear, count(*) as numMovies from %s m where m.director = 'Steven Spielberg' and m.startyear is not null group by m.startyear", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(29, tuples.size());

        tuples.forEach(t -> log.info("{}", t));
    }

    @Test
    public void testComplexAggregateQuery() {
        String sql = String.format("select t.director, (t.startyear - t.birthyear) as director_age from %s t where t.startyear is not null and t.birthyear is not null order by director_age desc limit 1", TABLE_NAME);

        IAlgebraOperator root = new SQLQueryParser().parse(sql);
        assertNotNull(root);

        ITupleIterator tupleIterator = root.execute(null, db);
        List<Tuple> tuples = TestUtils.toTupleList(tupleIterator);
        assertEquals(1, tuples.size());

        Tuple tuple = tuples.getFirst();
        assertEquals("Peter Lykke-Seest", tuple.getCells().get(1).getValue().getPrimitiveValue());
        // By default, the subtraction expression attribute ref is computed using the type "real"
        assertEquals(148.0, tuple.getCells().get(2).getValue().getPrimitiveValue());
    }
}
