package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import galois.test.utils.TestUtils;
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

@Slf4j
public class TestLLMAlgebra {
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
    public void testLLMScan() {
        // Query: SELECT * FROM actor
        TableAlias tableAlias = new TableAlias("actor");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralTableQueryExecutor(), null);
        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanOrderBy() {
        // Query: SELECT a.name FROM actor a WHERE gender = "Female" ORDER BY name
        TableAlias tableAlias = new TableAlias("actor", "a");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralTableQueryExecutor(), null);

        Expression exp = new Expression("gender == \"Female\"");
        exp.setVariableDescription("gender", new AttributeRef(tableAlias, "gender"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        AttributeRef attributeRef = new AttributeRef(tableAlias, "name");
        IAlgebraOperator orderBy = new OrderBy(List.of(attributeRef));
        orderBy.addChild(select);

        ITupleIterator tuples = orderBy.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

}
