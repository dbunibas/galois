package galois.optimizer.optimizations;

import galois.llm.algebra.LLMScan;
import galois.llm.query.IQueryExecutor;
import galois.optimizer.IOptimization;
import galois.optimizer.OptimizerException;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;

import static galois.optimizer.PromptOptimizer.optimizePrompt;

@Slf4j
public class AllConditionsPushdown implements IOptimization {
    @Override
    public IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query) {
        checkQuery(query);
        Select select = (Select) query;
        LLMScan scan = (LLMScan) query.getChildren().get(0);
        IQueryExecutor optimizedScan = toOptimizedScan(scan, select);
        return new LLMScan(scan.getTableAlias(), optimizedScan, scan.getAttributesSelect(), scan.getNormalizationStrategy(), scan.getLlmProbThreshold());
    }

    // TODO: Validate method
    private void checkQuery(IAlgebraOperator query) {
        boolean wrongInstance = !(query instanceof Select);
        boolean wrongChild = query.getChildren().size() != 1 && query.getChildren().get(0) instanceof LLMScan;
        if (wrongInstance || wrongChild) throw new OptimizerException("Cannot apply AllConditionsPushdown to query: " + query);
    }

    private IQueryExecutor toOptimizedScan(LLMScan scan, Select select) {
        IQueryExecutor executor = scan.getQueryExecutor();
        return executor.toBuilder()
                .firstPrompt(optimizePrompt(executor.getFirstPrompt()))
                .iterativePrompt(optimizePrompt(executor.getIterativePrompt()))
                .attributesPrompt(optimizePrompt(executor.getAttributesPrompt()))
                .expression(select.getSelections().get(0))
                .contentRetriever(executor.getContentRetriever())
                .build();
    }
}
