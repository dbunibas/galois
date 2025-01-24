package floq.llm.algebra.config;

import floq.llm.query.IQueryExecutor;
import lombok.Data;

public class ScanConfiguration {

    private IQueryExecutor queryExecutor;
    private IQueryExecutorFactory queryExecutorFactory;
    private String normalizationStrategy;
    private Double llmProbThreshold;
    private Integer maxIterations;

    public ScanConfiguration(IQueryExecutor queryExecutor, IQueryExecutorFactory queryExecutorFactory, String normalizationStrategy, Double llmProbThreshold) {
        this.queryExecutor = queryExecutor;
        this.queryExecutorFactory = queryExecutorFactory;
        this.normalizationStrategy = normalizationStrategy;
        this.llmProbThreshold = llmProbThreshold;
    }

    public IQueryExecutor createQueryExecutor(IQueryExecutor base) {
        return queryExecutorFactory.create(base);
    }

    @FunctionalInterface
    public interface IQueryExecutorFactory {

        IQueryExecutor create(IQueryExecutor base);
    }

    public void setLlmProbThreshold(Double llmProbThreshold) {
        this.llmProbThreshold = llmProbThreshold;
    }

    public IQueryExecutor getQueryExecutor() {
        return queryExecutor;
    }

    public String getNormalizationStrategy() {
        return normalizationStrategy;
    }

    public Double getLlmProbThreshold() {
        return llmProbThreshold;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
