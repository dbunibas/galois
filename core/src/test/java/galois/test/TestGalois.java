package galois.test;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.database.LLMDB;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.OllamaLlama3KeyQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLLama3KeyQueryExecutor;
import galois.optimizer.AllConditionsPushdownOptimizer;
import galois.optimizer.IOptimizer;
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
        IAlgebraOperator operator = parser.parse(queryPlan, llm, OperatorsConfigurationParser.getDefault(), sql);

        ITupleIterator iterator = operator.execute(llm, null);

        TestUtils.toTupleStream(iterator).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testFullPipeline() {
        // TODO: This will be added in the executeQuery method once stable
        String sql = "select a.name from target.actor a where a.gender = 'Female' order by a.name";
        IDatabase llmDB = new LLMDB(accessConfiguration);

        IQueryPlanner<Document> planner = new PostgresXMLPlanner(accessConfiguration);
        Document queryPlan = planner.planFrom(sql);

        IQueryPlanParser<Document> parser = new PostgresXMLParser();
        IQueryExecutor executor = new OllamaLlama3KeyQueryExecutor();
        ScanConfiguration scanConfiguration = new ScanConfiguration(executor, (ignored) -> new OllamaLlama3KeyQueryExecutor(), null, null);
        OperatorsConfiguration operatorsConfiguration = new OperatorsConfiguration(scanConfiguration);
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, operatorsConfiguration, sql);

        IOptimizer optimizer = new AllConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, operator);

        ITupleIterator iterator = optimizedQuery.execute(llmDB, null);
        TestUtils.toTupleStream(iterator).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testFullPipelineTogetherAI() {
        // TODO: This will be added in the executeQuery method once stable
        String sql = "select a.name from target.actor a where a.gender = 'Female' order by a.name";
        IDatabase llmDB = new LLMDB(accessConfiguration);

        IQueryPlanner<Document> planner = new PostgresXMLPlanner(accessConfiguration);
        Document queryPlan = planner.planFrom(sql);

        IQueryPlanParser<Document> parser = new PostgresXMLParser();
        IQueryExecutor executor = new TogetheraiLLama3KeyQueryExecutor();
        ScanConfiguration scanConfiguration = new ScanConfiguration(executor, (ignored) -> new OllamaLlama3KeyQueryExecutor(), null, null);
        OperatorsConfiguration operatorsConfiguration = new OperatorsConfiguration(scanConfiguration);
        IAlgebraOperator operator = parser.parse(queryPlan, llmDB, operatorsConfiguration, sql);

        IOptimizer optimizer = new AllConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, operator);

        ITupleIterator iterator = optimizedQuery.execute(llmDB, null);
        TestUtils.toTupleStream(iterator).map(Tuple::toString).forEach(log::info);
    }
}
