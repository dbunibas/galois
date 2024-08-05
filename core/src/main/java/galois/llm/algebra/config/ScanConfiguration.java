package galois.llm.algebra.config;

import galois.llm.query.IQueryExecutor;
import lombok.Data;

@Data
public class ScanConfiguration {
    private final IQueryExecutor queryExecutor;
    private final String normalizationStrategy;
}
