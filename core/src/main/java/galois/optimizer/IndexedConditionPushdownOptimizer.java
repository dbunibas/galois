package galois.optimizer;

import galois.optimizer.optimizations.SingleConditionPushdown;
import galois.parser.ParserWhere;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class IndexedConditionPushdownOptimizer implements IOptimizer {
    private final int index;

    @Override
    public IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query) {
        // TODO: Add scan to abstract component
        log.warn("Naïve implementation, for testing purposes only!");
        IAlgebraOperator currentNode = query.clone();
        while (currentNode != null) {
            if (currentNode instanceof Select) {
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(sql);
                List<Expression> expressions = parserWhere.getExpressions();
                Expression pushdownCondition = expressions.remove(index);
                log.debug("pushdownCondition is: {}", pushdownCondition);
                SingleConditionPushdown singleConditionPushdown = new SingleConditionPushdown(pushdownCondition, expressions, parserWhere.getOperation());
                IAlgebraOperator optimizedNode = singleConditionPushdown.optimize(database, currentNode);
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
}