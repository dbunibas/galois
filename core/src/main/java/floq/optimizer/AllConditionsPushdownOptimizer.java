package floq.optimizer;

import floq.optimizer.optimizations.AllConditionsPushdown;
import lombok.extern.slf4j.Slf4j;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Select;
import engine.model.database.IDatabase;

@Slf4j
public class AllConditionsPushdownOptimizer implements IOptimizer {
    private final AllConditionsPushdown allConditionsPushdown = new AllConditionsPushdown();
    private boolean removeFromAlgebraTree;

    public AllConditionsPushdownOptimizer(boolean removeFromAlgebraTree) {
        this.removeFromAlgebraTree = removeFromAlgebraTree;
    }

    @Override
    public String getName() {
        return IOptimizer.super.getName() + " - removeFromAlgebraTree: " + this.removeFromAlgebraTree;
    }
    
    

    @Override
    public IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query) {
        // TODO: Add scan to abstract component
        log.warn("Naïve implementation, for testing purposes only!");
        IAlgebraOperator root = query.clone();
        IAlgebraOperator currentNode = root;
        while (currentNode != null) {
            if (currentNode instanceof Select) {
                IAlgebraOperator optimizedNode = allConditionsPushdown.optimize(database, currentNode);
                if (removeFromAlgebraTree) {
                    IAlgebraOperator father = currentNode.getFather();
                    if (father == null) {
                        return optimizedNode;
                    }
                    IAlgebraOperator nodeToReplace = currentNode;
                    father.getChildren().replaceAll(n -> n.equals(nodeToReplace) ? optimizedNode : n);
                    currentNode = optimizedNode;
                } else {
                    currentNode.getChildren().clear();
                    currentNode.addChild(optimizedNode);
                    currentNode = optimizedNode;
                }
            }
            currentNode = currentNode.getChildren().isEmpty() ? null : currentNode.getChildren().get(0);
        }
        return root;
    }
}