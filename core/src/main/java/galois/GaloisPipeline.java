package galois;

import galois.optimizer.AllConditionsPushdownOptimizer;
import galois.optimizer.IOptimizer;
import galois.optimizer.IndexedConditionPushdownOptimizer;
import galois.optimizer.QueryPlan;
import galois.optimizer.estimators.ConfidenceEstimator;
import galois.parser.ParserFrom;
import galois.parser.ParserWhere;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import speedy.model.database.IDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class GaloisPipeline {
    public QueryPlan estimateBestPlan(IDatabase database, String sql) {
        ParserFrom parserFrom = new ParserFrom();
        parserFrom.parseFrom(sql);

        List<String> tables = parserFrom.getTables();
        String query = sql;
        query = query.replace("target.", "");

        ConfidenceEstimator estimator = new ConfidenceEstimator();
        Map<String, String> estimation = estimator.getEstimationForQuery2(database, tables, query);

        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(sql);
        List<Expression> expressions = parserWhere.getExpressions();
        List<Expression> expressionPushDown = new ArrayList<>();
        int indexPushDown = 0;
        List<Integer> indexes = new ArrayList<>();
        for (Expression expression : expressions) {
            for (String attr : estimation.keySet()) {
                if (estimation.get(attr).contains("high") && expression.toString().contains(attr)) {
                    expressionPushDown.add(expression);
                    indexes.add(indexPushDown);
                }
            }
            indexPushDown++;
        }
        Double confidence = estimator.getEstimationConfidence(database, tables, query, expressionPushDown);

        log.info("Pushdown: {}", expressionPushDown);
        log.info("Confidence Keys: {}", confidence);

        return new QueryPlan(expressionPushDown, confidence, expressions.size(), indexes);
    }

    public IOptimizer selectOptimizer(QueryPlan plan) {
        return selectOptimizer(plan, false);
    }

    public IOptimizer selectOptimizer(QueryPlan plan, boolean removeFromAlgebraTree) {
        String pushDownStrategy = plan.computePushdown();

        if (pushDownStrategy.equals(QueryPlan.PUSHDOWN_ALL_CONDITION)) {
            return new AllConditionsPushdownOptimizer(removeFromAlgebraTree);
        }

        if (pushDownStrategy.startsWith(QueryPlan.PUSHDOWN_SINGLE_CONDITION)) {
            Integer index = plan.getIndexPushDown();
            if (index == null) return null;
            return new IndexedConditionPushdownOptimizer(index, removeFromAlgebraTree);
        }

        return null;
    }
}
