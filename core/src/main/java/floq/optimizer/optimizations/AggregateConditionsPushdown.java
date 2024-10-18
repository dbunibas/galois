package floq.optimizer.optimizations;

import floq.llm.algebra.LLMScan;
import floq.llm.query.IQueryExecutor;
import floq.optimizer.IOptimization;
import floq.optimizer.OptimizerException;
import lombok.extern.slf4j.Slf4j;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Intersection;
import engine.model.algebra.Select;
import engine.model.algebra.Union;
import engine.model.database.*;
import engine.model.expressions.Expression;

import java.util.List;

import static floq.optimizer.PromptOptimizer.optimizePrompt;

@Slf4j
public class AggregateConditionsPushdown implements IOptimization {
    private final List<net.sf.jsqlparser.expression.Expression> conditions;
    private final String operation;

    public AggregateConditionsPushdown(List<net.sf.jsqlparser.expression.Expression> conditions, String operation) {
        if (conditions.size() < 2) {
            throw new IllegalArgumentException("At least two conditions are needed for aggregation!");
        }
        this.conditions = conditions;
        this.operation = operation;
    }

    @Override
    public IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query) {
        checkQuery(query);
        LLMScan scan = (LLMScan) query.getChildren().get(0);
        IAlgebraOperator left = toOptimizedScan(database, scan, conditions.get(0));
        IAlgebraOperator right;
        for (int i = 1; i < conditions.size(); i++) {
            right = toOptimizedScan(database, scan, conditions.get(i));
            left = aggregate(left, right);
        }
        return left;
    }

    // TODO: Validate method
    private void checkQuery(IAlgebraOperator query) {
        boolean wrongInstance = !(query instanceof Select);
        boolean wrongChild = query.getChildren().size() != 1 && query.getChildren().get(0) instanceof LLMScan;
        if (wrongInstance || wrongChild)
            throw new OptimizerException("Cannot apply SingleConditionPushdown to query: " + query);
    }

    private IAlgebraOperator toOptimizedScan(IDatabase database, LLMScan scan, net.sf.jsqlparser.expression.Expression condition) {
        TableAlias tableAlias = scan.getTableAlias();
        ITable table = database.getTable(tableAlias.getTableName());

        String conditionAsString = condition.toString();
        // TODO: Add this to utility (same code in FilterParser.java)
        Expression pushdownExpression = new Expression(cleanExpression(conditionAsString));
        log.debug("Optimized pushdown expression: {}", pushdownExpression);
        addAllAttributesToExpression(pushdownExpression, table, tableAlias);

        IQueryExecutor executor = scan.getQueryExecutor();
        IQueryExecutor optimizedExecutor = executor.toBuilder()
                .firstPrompt(optimizePrompt(executor.getFirstPrompt()))
                .iterativePrompt(optimizePrompt(executor.getIterativePrompt()))
                .attributesPrompt(optimizePrompt(executor.getAttributesPrompt()))
                .expression(pushdownExpression)
                .contentRetriever(executor.getContentRetriever())
                .build();
        return new LLMScan(scan.getTableAlias(), optimizedExecutor, scan.getAttributesSelect(), scan.getNormalizationStrategy());
    }

    private IAlgebraOperator aggregate(IAlgebraOperator left, IAlgebraOperator right) {
        IAlgebraOperator aggregation = operation.equalsIgnoreCase("OR") ? new Union() : new Intersection();
        aggregation.addChild(left);
        aggregation.addChild(right);
        return aggregation;
    }

    private void addAllAttributesToExpression(Expression expression, ITable table, TableAlias tableAlias) {
        // TODO: Add this to utility (same code in FilterParser.java), but pay attention to the variable name in variable description (table alias is missing when parsing using the ParserWhere)
        for (Attribute attribute : table.getAttributes()) {
            String variable = attribute.getName();
            expression.setVariableDescription(variable, new AttributeRef(tableAlias, attribute.getName()));
        }
    }

    private String cleanExpression(String text) {
        // TODO: Handle all cases by using a map! (TestParseXML::testWhereWithBooleanOperationsNot should than work)
        return text
                .replaceAll("::text", "")
                .replaceAll("'", "\"")
                .replaceAll("=", "==")
                .replaceAll("AND", "&&")
                .replaceAll("OR", "||");
    }
}
