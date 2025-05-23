package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.OllamaLlama3KeyScanQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3TableQueryExecutor;
import galois.optimizer.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.*;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

import static galois.test.utils.TestUtils.toTupleStream;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestOptimizer {
    private IDatabase llmDB;

    @BeforeEach
    public void setUp() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_llm_actors";
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
    @Disabled
    public void testConditionPushDown() {
        // Query: SELECT * FROM actor WHERE gender = 'male'
        TableAlias tableAlias = new TableAlias("actor", "a");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLlama3KeyScanQueryExecutor(), null);

        Expression exp = new Expression("gender == \"Female\"");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimization optimizer = new LLMHistogramOptimizer();
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, select);

        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    @Disabled
    public void testConditionPushDownOrderBy() {
        // Query: SELECT * FROM actor WHERE gender = 'male' ORDER_BY name
        TableAlias tableAlias = new TableAlias("actor", "a");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLlama3KeyScanQueryExecutor(), null);

        Expression exp = new Expression("gender == \"Female\"");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        AttributeRef attributeRef = new AttributeRef(tableAlias, "name");
        IAlgebraOperator orderBy = new OrderBy(List.of(attributeRef));
        orderBy.addChild(select);

        IOptimization optimizer = new LLMHistogramOptimizer();
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, orderBy);

        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testAllConditionsPushdownOptimizer() {
        String sql = "select * from actor where gender = 'Female' && birth_year > 1980";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = OllamaLlama3KeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" && birth_year > 1980");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new AllConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);

        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());
        assertInstanceOf(LLMScan.class, optimizedQuery.getChildren().get(0));
        assertEquals(0, optimizedQuery.getChildren().get(0).getChildren().size());

//        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
//        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testIndexedConditionPushdownOptimizer() {
        String sql = "select * from actor where gender = 'Female' && birth_year > 1980";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = OllamaLlama3KeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" && birth_year > 1980");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new IndexedConditionPushdownOptimizer(0, false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);

        // Top level select
        assertEquals(exp, ((Select) optimizedQuery).getSelections().get(0));
        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());

        // Remaining conditions select
        IAlgebraOperator child = optimizedQuery.getChildren().get(0);
        assertInstanceOf(Select.class, child);
        assertEquals(1, child.getChildren().size());

        // Scan with pushdown condition
        child = child.getChildren().get(0);
        assertInstanceOf(LLMScan.class, child);
        assertEquals(0, child.getChildren().size());

//        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
//        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testIndexedConditionPushdownOptimizerMultiple() {
        String sql = "select * from actor where gender = 'Female' && birth_year > 1980 && name = 'Scarlett Johansson'";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = OllamaLlama3KeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" && birth_year > 1980 && name == \"Scarlett Johansson\"");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        exp.setVariableDescription("name", new AttributeRef(tableAlias, "name"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new IndexedConditionPushdownOptimizer(0, false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);

        // Top level select
        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());
        assertEquals(exp, ((Select) optimizedQuery).getSelections().get(0));

        // Remaining conditions select
        IAlgebraOperator child = optimizedQuery.getChildren().get(0);
        assertInstanceOf(Select.class, child);
        assertEquals(1, child.getChildren().size());

        // Scan with pushdown condition
        child = child.getChildren().get(0);
        assertInstanceOf(LLMScan.class, child);
        assertEquals(0, child.getChildren().size());

//        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
//        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testAggregateConditionsSingleOR() {
        String sql = "select * from actor where gender = 'Female' OR birth_year > 1980";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = TogetheraiLlama3TableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" || birth_year > 1980");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new AggregateConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);
        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());

        IAlgebraOperator child = optimizedQuery.getChildren().get(0);
        assertInstanceOf(Union.class, child);
        assertEquals(2, child.getChildren().size());
        assertInstanceOf(LLMScan.class, child.getChildren().get(0));
        assertInstanceOf(LLMScan.class, child.getChildren().get(1));

//        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
//        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testAggregateConditionsSingleAND() {
        String sql = "select * from actor where gender = 'Female' AND birth_year > 1980";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = TogetheraiLlama3TableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" && birth_year > 1980");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new AggregateConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);
        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());

        IAlgebraOperator child = optimizedQuery.getChildren().get(0);
        assertInstanceOf(Intersection.class, child);
        assertEquals(2, child.getChildren().size());
        assertInstanceOf(LLMScan.class, child.getChildren().get(0));
        assertInstanceOf(LLMScan.class, child.getChildren().get(1));

//        ITupleIterator tuples = optimizedQuery.execute(llmDB, null);
//        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testAggregateConditionsMultipleOR() {
        String sql = "select * from actor where gender = 'Female' OR birth_year > 1980 OR name = 'Robert De Niro'";

        TableAlias tableAlias = new TableAlias("actor", "a");
        IQueryExecutor executor = TogetheraiLlama3TableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);

        Expression exp = new Expression("gender == \"Female\" || birth_year > 1980 || name == \"Robert De Niro\"");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        exp.setVariableDescription("birth_year", new AttributeRef(tableAlias, "birth_year"));
        exp.setVariableDescription("name", new AttributeRef(tableAlias, "name"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        IOptimizer optimizer = new AggregateConditionsPushdownOptimizer(false);
        IAlgebraOperator optimizedQuery = optimizer.optimize(llmDB, sql, select);
        assertInstanceOf(Select.class, optimizedQuery);
        assertEquals(1, optimizedQuery.getChildren().size());

        IAlgebraOperator child = optimizedQuery.getChildren().get(0);
        assertInstanceOf(Union.class, child);
        assertEquals(2, child.getChildren().size());
        assertInstanceOf(Union.class, child.getChildren().get(0));
        assertInstanceOf(LLMScan.class, child.getChildren().get(1));

        IAlgebraOperator left = child.getChildren().get(0);
        assertEquals(2, left.getChildren().size());
        assertInstanceOf(LLMScan.class, left.getChildren().get(0));
        assertInstanceOf(LLMScan.class, left.getChildren().get(1));
    }
}
