package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.ollama.OllamaMistralTableQueryExecutor;
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
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralTableQueryExecutor());
        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanOrderBy() {
        // Query: SELECT name, sex FROM actor WHERE sex = "female" ORDER BY name
        TableAlias tableAlias = new TableAlias("actor");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralTableQueryExecutor());

        Expression exp = new Expression("sex == \"female\"");
        exp.setVariableDescription("sex", new AttributeRef(tableAlias, "sex"));
        Select select = new Select(exp);
        select.addChild(llmScan);

        AttributeRef attributeRef = new AttributeRef(tableAlias, "name");
        IAlgebraOperator orderBy = new OrderBy(List.of(attributeRef));
        orderBy.addChild(select);

        ITupleIterator tuples = orderBy.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

}
