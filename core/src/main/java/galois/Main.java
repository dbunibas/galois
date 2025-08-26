package galois;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galois.sqlparser.SQLQueryParser;
import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.openai.OpenAIKeyScanQueryExecutor;
import galois.llm.query.openai.OpenAITableQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3KeyScanQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3TableQueryExecutor;
import galois.optimizer.IOptimizer;
import galois.optimizer.QueryPlan;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import java.io.File;
import java.io.IOException;

@Slf4j
public class Main {
    private static final double DEFAULT_THRESHOLD = 0.6;

    public static void main(String[] args) throws IOException {
        new Main().execute(args);
    }

    private void execute(String[] args) throws IOException {
        if (args.length != 1) {
            log.error("The only argument should be the absolute query path!");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            log.error("The requested file doesn't exists!");
            return;
        }

        Query query = new ObjectMapper().readValue(file, Query.class);
        double confidenceThreshold = query.confidenceThreshold == null ? DEFAULT_THRESHOLD : query.confidenceThreshold;
        IDatabase database = readDatabase(query);

        GaloisPipeline pipeline = new GaloisPipeline();
        QueryPlan plan = pipeline.estimateBestPlan(database, query.sql);
        IOptimizer optimizer = pipeline.selectOptimizer(plan);
        IQueryExecutor executor = selectExecutor(plan, confidenceThreshold, Configuration.getInstance().getLLMProvider());
        log.info("Executor: {}", executor.getClass().getSimpleName());

        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        IAlgebraOperator operator = sqlQueryParser.parse(query.sql, (tableAlias, ignored) -> new LLMScan(tableAlias, executor, null));

        ITupleIterator iterator;
        if (optimizer != null) {
            log.info("Optimizer: {}", optimizer.getName());
            IAlgebraOperator optimizedOperator = optimizer.optimize(database, query.sql, operator);
            log.info("Executing the optimized query");
            iterator = optimizedOperator.execute(database, database);
        } else {
            log.info("Executing the unoptimized query");
            iterator = operator.execute(database, database);
        }

        log.info("**** Results");
        Iterable<Tuple> iterable = () -> iterator;
        for (Tuple tuple : iterable) {
            log.info("| {}", tuple);
        }
        log.info("****");
    }

    @NotNull
    private static IDatabase readDatabase(Query query) {
        var accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(query.databaseDriver);
        accessConfiguration.setUri(query.databaseURI);
        accessConfiguration.setSchemaName(query.databaseSchema);
        accessConfiguration.setLogin(query.databaseUser);
        accessConfiguration.setPassword(query.databasePassword);
        return new LLMDB(accessConfiguration);
    }

    private IQueryExecutor selectExecutor(QueryPlan plan, double threshold, String provider) {
        Double confidence = plan.getConfidenceKeys();
        boolean useKeyScan = confidence != null && confidence > threshold;
        return switch (provider.trim().toLowerCase()) {
            case Constants.PROVIDER_TOGETHERAI ->
                    useKeyScan ? new TogetheraiLlama3KeyScanQueryExecutor() : new TogetheraiLlama3TableQueryExecutor();
            case Constants.PROVIDER_OPENAI ->
                    useKeyScan ? new OpenAIKeyScanQueryExecutor() : new OpenAITableQueryExecutor();
            default -> throw new IllegalArgumentException("Unknown provider!");
        };
    }

    private record Query(
            String databaseDriver,
            String databaseURI,
            String databaseSchema,
            String databaseUser,
            String databasePassword,
            String sql,
            Double confidenceThreshold
    ) {
    }
}
