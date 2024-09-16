package galois.optimizer;

import galois.parser.ParserProvenance;
import galois.parser.ParserWhere;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;

@Slf4j
public class LogicalPlanOptimizer {

    private String optimizations = "";

    public IAlgebraOperator optimizeByConfidence(IAlgebraOperator operator, String sqlQuery, IDatabase database, Map<ITable, Map<Attribute, Double>> dbConfidence, double threshold, boolean removeFromAlgebraTree) {
        this.optimizations = "";
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(sqlQuery);
        if (parserWhere.getOperation().equals("OR")) {
            log.debug("Cannot optimize tree with OR operation");
            return operator;
        }
        List<Expression> expressions = parserWhere.getExpressions();
        if (expressions == null || expressions.isEmpty()) {
            log.debug("No expressions to optimize");
            return operator;
        }
        ParserProvenance parserProvenance = new ParserProvenance(database);
        parserProvenance.parse(sqlQuery);
        Set<String> tablesInQuery = parserProvenance.getTablesProvenance();
        List<Integer> indexToOptimize = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            Expression expression = expressions.get(i);
            double confidenceForExpression = findAttributeConfidence(expression, tablesInQuery, dbConfidence);
            if (confidenceForExpression >= threshold) {
                log.debug("PushDown: {} with confidence: {}", expression, confidenceForExpression);
                optimizations += expression.toString() + "(" + confidenceForExpression + ") ";
                indexToOptimize.add(i);
            }
        }
        if (indexToOptimize.isEmpty()) {
            return operator;
        }
        log.debug("Executing optimization on the algebra tree");
        indexToOptimize = indexToOptimize.reversed();
        for (Integer index : indexToOptimize) {
            log.debug("Pushdown with index: {}", index);
            IndexedConditionPushdownOptimizer optimizer = new IndexedConditionPushdownOptimizer(index, removeFromAlgebraTree);
            operator = optimizer.optimize(database, sqlQuery, operator);
        }
        return operator;
    }

    public String getOptimizations() {
        return optimizations;
    }

    // TODO we assume expression involve only one attribute
    private double findAttributeConfidence(Expression expression, Set<String> tablesInQuery, Map<ITable, Map<Attribute, Double>> dbConfidence) {
        String expressionString = expression.toString();
        for (ITable table : dbConfidence.keySet()) {
            if (!tablesInQuery.contains(table.getName())) {
                continue;
            }
            Map<Attribute, Double> confidencePerTable = dbConfidence.get(table);
            for (Attribute attribute : confidencePerTable.keySet()) {
                if (expressionString.contains(attribute.getName())) {
                    return confidencePerTable.get(attribute);
                }
            }
        }
        return 0.0;
    }

}
