package galois.optimizer;

import galois.optimizer.optimizations.SingleConditionPushdown;
import galois.parser.ParserWhere;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;

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
                pushdownCondition = expressions.remove(index);
                log.debug("pushdownCondition is: {}", pushdownCondition);
                SingleConditionPushdown singleConditionPushdown = new SingleConditionPushdown(pushdownCondition, expressions, parserWhere.getOperation());
                IAlgebraOperator optimizedNode = singleConditionPushdown.optimize(database, currentNode);
                if (removeFromAlgebraTree) {
                    IAlgebraOperator father = currentNode.getFather();
                    if (father == null) {
                        return optimizedNode;
                    }
                    // TODO: Add replace children?
                    father.getChildren().clear();
                    father.addChild(optimizedNode);
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
