package floq.optimizer;

import floq.optimizer.optimizations.SingleConditionPushdown;
import floq.parser.ParserWhere;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Select;
import engine.model.database.IDatabase;

import java.util.List;

@Slf4j
public class IndexedConditionPushdownOptimizer implements IOptimizer {

    private final int index;
    private Expression pushdownCondition;
    private boolean removeFromAlgebraTree;

    public IndexedConditionPushdownOptimizer(int index, boolean removeFromAlgebraTree) {
        this.index = index;
        this.removeFromAlgebraTree = removeFromAlgebraTree;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName() + " - index " + index + " - pushdown \"" + pushdownCondition + "\"" + " removeFromAlgebraTree - " + removeFromAlgebraTree;
    }

    @Override
    public IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query) {
        // TODO: Add scan to abstract component
        log.warn("Naïve implementation, for testing purposes only!");
        IAlgebraOperator root = query.clone();
        IAlgebraOperator currentNode = root;
        while (currentNode != null) {
            if (currentNode instanceof Select) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(sql);
                if (parserWhere.getOperation().equals("OR")) {
                    log.debug("Cannot optimize tree with OR operation");
                    return currentNode;
                }
                List<Expression> expressions = parserWhere.getExpressions();
                log.debug("Expressions is: {}", expressions);
                pushdownCondition = expressions.remove(index);
                log.debug("pushdownCondition is: {}", pushdownCondition);
                log.debug("Remaining Expressions is: {}", expressions);
                SingleConditionPushdown singleConditionPushdown = new SingleConditionPushdown(pushdownCondition, expressions, parserWhere.getOperation());
                IAlgebraOperator optimizedNode = singleConditionPushdown.optimize(database, currentNode);
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
