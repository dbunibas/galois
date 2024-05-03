package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.llamacpp.LlamaCppKeyAttributesQueryExecutor;
import galois.llm.query.ollama.OllamaLLama3KeyAttributesQueryExecutor;
import galois.llm.query.ollama.OllamaLLama3KeyValuesQueryExecutor;
import galois.llm.query.ollama.OllamaLlama3TableQueryExecutor;
import galois.llm.query.outlines.OutlinesKeyAttributesQueryExecutor;
import galois.llm.query.outlines.OutlinesKeyValueQueryExecutor;
import galois.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

@Slf4j
public class TestScanStrategies {
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
    public void testLLMScanUsingLLama3Table() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLlama3TableQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanUsingLLama3KeyAttributes() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLLama3KeyAttributesQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanUsingLLama3KeyValues() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaLLama3KeyValuesQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanUsingOutlinesKeyAttributes() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OutlinesKeyAttributesQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanUsingLlamaCppKeyAttributes() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new LlamaCppKeyAttributesQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testLLMScanUsingOutlinesKeyValue() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OutlinesKeyValueQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }
}
