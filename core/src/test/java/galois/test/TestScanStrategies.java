package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.llamacpp.LlamaCppKeyAttributesQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.llm.query.ollama.mistral.OllamaMistralNLQueryExecutor;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
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
    public void testLLMScanUsingMistralTable() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralTableQueryExecutor());

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
    public void testLLMScanUsingOutlinesKeyValue() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OutlinesKeyValueQueryExecutor());

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
    public void testOllamaLlama3NLQuery() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");

        IQueryExecutor executor = OllamaLlama3NLQueryExecutor.builder()
                .naturalLanguagePrompt("List the name, gender and birth year of some film directors")
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testOllamaLlama3SQLQuery() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");

        IQueryExecutor executor = OllamaLlama3SQLQueryExecutor.builder()
                .sql("select * from film_director")
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testOllamaLlama3Table() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");

        IQueryExecutor executor = OllamaLlama3TableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testOllamaLLama3Key() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");

        IQueryExecutor executor = OllamaLLama3KeyQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testOllamaLLama3KeyScan() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");

        IQueryExecutor executor = OllamaLLama3KeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    @Test
    public void testOllamaMistralNLQuery() {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, new OllamaMistralNLQueryExecutor());

        ITupleIterator tuples = llmScan.execute(llmDB, null);
        TestUtils.toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }
}
