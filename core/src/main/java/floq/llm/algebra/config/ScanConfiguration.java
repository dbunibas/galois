package floq.llm.algebra.config;

import floq.llm.query.IQueryExecutor;
import lombok.Data;

@Data
public class ScanConfiguration {
    private final IQueryExecutor queryExecutor;
    private final IQueryExecutorFactory queryExecutorFactory;
    private final String normalizationStrategy;

    public IQueryExecutor createQueryExecutor(IQueryExecutor base) {
        return queryExecutorFactory.create(base);
    }

    @FunctionalInterface
    public interface IQueryExecutorFactory {
        IQueryExecutor create(IQueryExecutor base);
    }
}
