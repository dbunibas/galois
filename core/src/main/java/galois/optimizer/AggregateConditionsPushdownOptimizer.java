package galois.optimizer;

import galois.optimizer.optimizations.AggregateConditionsPushdown;
import galois.parser.ParserWhere;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.IDatabase;

import java.util.List;

@Slf4j
public class AggregateConditionsPushdownOptimizer implements IOptimizer {
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
                currentNode.getChildren().clear();
                currentNode.addChild(optimizedNode);
//                IAlgebraOperator father = currentNode.getFather();
//                if (father == null) {
//                    return currentNode;
//                }
                // TODO: Add replace children?
//                father.getChildren().clear();
//                father.addChild(currentNode);
//                currentNode = optimizedNode;
            }
            currentNode = currentNode.getChildren().isEmpty() ? null : currentNode.getChildren().get(0);
        }
        return root;
    }
}