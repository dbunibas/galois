package galois.test;

import galois.llm.database.LLMDB;
import galois.parser.IQueryPlanParser;
import galois.parser.postgresql.PostgresXMLParser;
import galois.planner.IQueryPlanner;
import galois.planner.postgresql.xml.PostgresXMLPlanner;
import galois.test.experiments.json.parser.OperatorsConfigurationParser;
import galois.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

@Slf4j
public class TestGalois {
    private static AccessConfiguration accessConfiguration;

    @BeforeAll
    public static void beforeAll() {
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_llm_actors";
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
        executeQuery(sql);
    }

    @Test
    public void testSimpleOrderBy() {
        String sql = "select * from target.actor a order by a.name";
        executeQuery(sql);
    }

    @Test
    public void testSimpleFilter() {
        String sql = "select * from target.actor a where a.gender = 'Female'";
        executeQuery(sql);
    }

    @Test
    public void testSimpleFilterWithProjection() {
        String sql = "select a.name, a.birth_year from target.actor a where a.birth_year > 1960 order by a.birth_year";
        executeQuery(sql);
    }

    @Test
    public void testSimpleJoin() {
        String sql = "select * from target.film f join target.film_director fd on f.director = fd.name";
        executeQuery(sql);
    }

    @Test
    public void testSimpleJoinWithFilter() {
        String sql = """
                select *
                from target.film f join target.film_director fd on f.director = fd.name
                where f.year > 2008
                """;
        executeQuery(sql);
    }

    private void executeQuery(String sql) {
        IDatabase llm = new LLMDB(accessConfiguration);

        IQueryPlanner<Document> planner = new PostgresXMLPlanner(accessConfiguration);
        Document queryPlan = planner.planFrom(sql);

        IQueryPlanParser<Document> parser = new PostgresXMLParser();
        IAlgebraOperator operator = parser.parse(queryPlan, llm, OperatorsConfigurationParser.parseJSON(null));

        ITupleIterator iterator = operator.execute(llm, null);

        TestUtils.toTupleStream(iterator).map(Tuple::toString).forEach(log::info);
    }
}
