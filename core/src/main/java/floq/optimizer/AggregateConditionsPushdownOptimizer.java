package floq.optimizer;

import floq.optimizer.optimizations.AggregateConditionsPushdown;
import floq.parser.ParserWhere;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Select;
import engine.model.database.IDatabase;

import java.util.List;

@Slf4j
public class AggregateConditionsPushdownOptimizer implements IOptimizer {

    private boolean removeFromAlgebraTree;

    public AggregateConditionsPushdownOptimizer(boolean removeFromAlgebraTree) {
        this.removeFromAlgebraTree = removeFromAlgebraTree;
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
                List<Expression> expressions = parserWhere.getExpressions();
                log.debug("Parsed expressions: {}", expressions);
                AggregateConditionsPushdown aggregateConditionsPushdown = new AggregateConditionsPushdown(expressions, parserWhere.getOperation());
                IAlgebraOperator optimizedNode = aggregateConditionsPushdown.optimize(database, currentNode);
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
