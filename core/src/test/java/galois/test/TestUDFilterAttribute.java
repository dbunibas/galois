package galois.test;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.SQLQueryParser;
import galois.udf.GaloisUDFFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.persistence.DAOMainMemoryDatabase;

import java.util.List;

import static galois.test.utils.TestUtils.toTupleList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class TestUDFilterAttribute {
    private static final String TABLE_NAME = "FamousPeopleTable";
    private static final IUserDefinedFunctionFactory GALOIS_UDF_FACTORY = new GaloisUDFFactory();

    private static MainMemoryDB mainMemoryDB;

    @BeforeAll
    public static void beforeAll() {
        String schema = requireNonNull(TestUDFilterAttribute.class.getResource("/udf_famous_people/schema.xsd")).getFile();
        String instance = requireNonNull(TestUDFilterAttribute.class.getResource("/udf_famous_people/tuples.xml")).getFile();
        mainMemoryDB = new DAOMainMemoryDatabase().loadXMLDatabase(schema, instance);
    }

    @Test
    public void testWrongSyntax00() {
        String sql = String.format("select * from %s f where udfilterattr(f.name)", TABLE_NAME);
        assertThrows(IllegalArgumentException.class, () -> executeQueryMainMemory(sql));
    }

    @Test
    public void testTennisPlayers() {
        String sql = String.format("select * from %s f where udfilterattr('is_tennis_player')", TABLE_NAME);
        List<Tuple> result = executeQueryMainMemory(sql);
        assertEquals(2, result.size());
        log.info("{}", result);
    }

    private List<Tuple> executeQueryMainMemory(String sql) {
        IAlgebraOperator operator = new SQLQueryParser().parse(sql, GALOIS_UDF_FACTORY);
        ITupleIterator result = operator.execute(mainMemoryDB, mainMemoryDB);
        return toTupleList(result);
    }
}
