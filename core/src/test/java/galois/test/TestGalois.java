package galois.test;

import galois.llm.database.LLMDB;
import galois.parser.IQueryPlanParser;
import galois.parser.postgresql.PostgresXMLParser;
import galois.planner.IQueryPlanner;
import galois.planner.postgresql.xml.PostgresXMLPlanner;
import org.jdom2.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestGalois {
    private static final Logger logger = LoggerFactory.getLogger(TestGalois.class);

    private static AccessConfiguration accessConfiguration;

    @BeforeAll
    public static void beforeAll() {
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_dummy_actors";
        String schemaName = "target";
        String username = "pguser";
        String password = "pguser";

        accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
    }

    @Test
    public void testSimpleSelect() {
        String sql = "select * from target.actor a";

        IQueryPlanner<Document> planner = new PostgresXMLPlanner(accessConfiguration);
        Document queryPlan = planner.planFrom(sql);

        IQueryPlanParser<Document> parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan);

        IDatabase llm = new LLMDB(accessConfiguration);
        ITupleIterator iterator = operator.execute(llm, null);

        toTupleStream(iterator).map(Tuple::toString).forEach(logger::info);
    }

    private Stream<Tuple> toTupleStream(ITupleIterator iterator) {
        Iterable<Tuple> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
