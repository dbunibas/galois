package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.OrderBy;
import speedy.model.algebra.Select;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestLLMAlgebra {
    private static final Logger logger = LoggerFactory.getLogger(TestLLMAlgebra.class);

    private IDatabase llmDB;

    @BeforeEach
    public void setUp() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_dummy_actors";
        String schemaName = "target";
        String username = "pguser";
        String password = "pguser";
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
        llmDB = new LLMDB(accessConfiguration);
    }

    @Test
    public void testLLMScan() {
        // Query: SELECT * FROM actor
        TableAlias tableAlias = new TableAlias("actor");
        IAlgebraOperator llmScan = new LLMScan(tableAlias);
        ITupleIterator tuples = llmScan.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(logger::info);
    }

    @Test
    public void testLLMScanOrderBy() {
        // Query: SELECT name, sex FROM actor WHERE sex = "female" ORDER BY name
        TableAlias tableAlias = new TableAlias("actor");
        IAlgebraOperator llmScan = new LLMScan(tableAlias);

        Expression exp = new Expression("sex == \"female\"");
        exp.setVariableDescription("sex", new AttributeRef(tableAlias, "sex"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        AttributeRef attributeRef = new AttributeRef(tableAlias, "name");
        IAlgebraOperator orderBy = new OrderBy(List.of(attributeRef));
        orderBy.addChild(select);

        ITupleIterator tuples = orderBy.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(logger::info);
    }

    private Stream<Tuple> toTupleStream(ITupleIterator iterator) {
        Iterable<Tuple> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
