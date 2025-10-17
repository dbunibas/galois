package bsf.optimizer;

import bsf.llm.algebra.LLMScan;
import bsf.optimizer.estimators.LLMCardinalityEstimator;
import bsf.optimizer.optimizations.AllConditionsPushdown;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;

@Slf4j
public class LLMHistogramOptimizer implements IOptimization {
    private final LLMCardinalityEstimator llmCardinalityEstimator = LLMCardinalityEstimator.builder().build();

    @Override
    public IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query) {
        // TODO: Add scan to abstract component
        log.warn("Naïve implementation, for testing purposes only!");
        IAlgebraOperator currentNode = query;
        while (currentNode != null) {
            if (currentNode instanceof Select) {
                IAlgebraOperator optimizedNode = processSelect(database, (Select) currentNode);
                IAlgebraOperator father = currentNode.getFather();
                if (father == null) {
                    return optimizedNode;
                }
                // TODO: Add replace children?
                father.getChildren().clear();
                father.addChild(optimizedNode);
                currentNode = optimizedNode;
            }
            currentNode = currentNode.getChildren().isEmpty() ? null : currentNode.getChildren().get(0);
        }
        return query;
    }

    private IAlgebraOperator processSelect(IDatabase database, Select currentNode) {
        LLMScan llmScan = (LLMScan) currentNode.getChildren().get(0);
        ITable table = database.getTable(llmScan.getTableAlias().getTableName());
        double tableEstimate = llmCardinalityEstimator.estimate(table);
        double conditionEstimate = llmCardinalityEstimator.estimateWithExpression(table, currentNode.getSelections().get(0));
        if (conditionEstimate < tableEstimate) {
            return new AllConditionsPushdown().optimize(database, currentNode);
        }
        return currentNode;
    }
}
