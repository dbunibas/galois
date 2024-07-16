package galois.optimizer;

import galois.optimizer.optimizations.AllConditionsPushdown;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;

@Slf4j
public class AllConditionsPushdownOptimizer implements IOptimizer {
    private final AllConditionsPushdown allConditionsPushdown = new AllConditionsPushdown();

    @Override
    public IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query) {
        // TODO: Add scan to abstract component
        log.warn("Naïve implementation, for testing purposes only!");
        IAlgebraOperator root = query.clone();
        IAlgebraOperator currentNode = root;
        while (currentNode != null) {
            if (currentNode instanceof Select) {
                IAlgebraOperator optimizedNode = allConditionsPushdown.optimize(database, currentNode);
                currentNode.getChildren().clear();
                currentNode.addChild(optimizedNode);
//                IAlgebraOperator father = currentNode.getFather();
//                if (father == null) {
//                    return optimizedNode;
//                }
//                // TODO: Add replace children?
//                father.getChildren().clear();
//                father.addChild(optimizedNode);
//                currentNode = optimizedNode;
            }
            currentNode = currentNode.getChildren().isEmpty() ? null : currentNode.getChildren().get(0);
        }
        return root;
    }
}
