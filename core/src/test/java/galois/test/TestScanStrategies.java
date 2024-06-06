package galois.test;

import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.llm.query.ollama.mistral.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import static galois.test.utils.TestUtils.toTupleStream;

@Slf4j
public class TestScanStrategies {
    private static final String NL_PROMPT = "List the name, gender and birth year of some film directors";
    private static final String SQL = "select * from film_director";

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
    public void testOllamaLlama3NLQuery() {
        IQueryExecutor executor = OllamaLlama3NLQueryExecutor.builder()
                .naturalLanguagePrompt(NL_PROMPT)
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaLlama3SQLQuery() {
        IQueryExecutor executor = OllamaLlama3SQLQueryExecutor.builder()
                .sql(SQL)
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaLlama3Table() {
        IQueryExecutor executor = OllamaLlama3TableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaLLama3Key() {
        IQueryExecutor executor = OllamaLlama3KeyQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaLLama3KeyScan() {
        IQueryExecutor executor = OllamaLlama3KeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaMistralNLQuery() {
        IQueryExecutor executor = OllamaMistralNLQueryExecutor.builder()
                .naturalLanguagePrompt(NL_PROMPT)
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaMistralSQLQuery() {
        IQueryExecutor executor = OllamaMistralSQLQueryExecutor.builder()
                .sql(SQL)
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaMistralTable() {
        IQueryExecutor executor = OllamaMistralTableQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaMistralKey() {
        IQueryExecutor executor = OllamaMistralKeyQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    @Test
    public void testOllamaMistralKeyScan() {
        IQueryExecutor executor = OllamaMistralKeyScanQueryExecutor.builder()
                .maxIterations(2)
                .build();
        testExecutor(executor);
    }

    private void testExecutor(IQueryExecutor executor) {
        // Query: SELECT * FROM film_director fd
        TableAlias tableAlias = new TableAlias("film_director", "fd");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor);
        ITupleIterator tuples = llmScan.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }
}
