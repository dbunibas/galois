package galois.llm.algebra.config;

import galois.llm.query.IQueryExecutor;
import lombok.Data;

@Data
public class ScanConfiguration {
    private final IQueryExecutor queryExecutor;
    private final IQueryExecutorFactory queryExecutorFactory;
    private final String normalizationStrategy;

    public IQueryExecutor createQueryExecutor() {
        return queryExecutorFactory.create();
    }

    @FunctionalInterface
    public interface IQueryExecutorFactory {
        IQueryExecutor create();
    }
}
