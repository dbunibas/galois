package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.ollama.llama3.OllamaLlama3KeyScanQueryExecutor;
import galois.optimizer.IOptimization;
import galois.optimizer.LLMHistogramOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static galois.test.utils.TestUtils.toTupleStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    public void testConditionPushDown() {
        // Query: SELECT * FROM actor WHERE gender = 'male'
        TableAlias tableAlias = new TableAlias("actor", "a");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLlama3KeyScanQueryExecutor());

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
    public void testConditionPushDownOrderBy() {
        // Query: SELECT * FROM actor WHERE gender = 'male' ORDER_BY name
        TableAlias tableAlias = new TableAlias("actor", "a");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLlama3KeyScanQueryExecutor());

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
}
