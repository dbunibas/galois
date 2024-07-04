package galois.optimizer.optimizations;

import galois.llm.algebra.LLMScan;
import galois.llm.query.IQueryExecutor;
import galois.optimizer.IOptimization;
import galois.optimizer.OptimizerException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.*;
import speedy.model.expressions.Expression;

import java.util.List;
import java.util.stream.Collectors;

import static galois.optimizer.PromptOptimizer.optimizePrompt;

@Slf4j
@AllArgsConstructor
public class SingleConditionPushdown implements IOptimization {
    private final net.sf.jsqlparser.expression.Expression pushdownCondition;
    private final List<net.sf.jsqlparser.expression.Expression> remainingConditions;
    private final String operation;

    @Override
    public IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query) {
        checkQuery(query);
        LLMScan scan = (LLMScan) query.getChildren().get(0);
        IAlgebraOperator optimizedScan = toOptimizedScan(database, scan);
        IAlgebraOperator optimizedSelect = toOptimizedSelect(database, scan);
        if (optimizedSelect == null) return optimizedScan;
        optimizedSelect.addChild(optimizedScan);
        return optimizedSelect;
    }

    // TODO: Validate method
    private void checkQuery(IAlgebraOperator query) {
        boolean wrongInstance = !(query instanceof Select);
        boolean wrongChild = query.getChildren().size() != 1 && query.getChildren().get(0) instanceof LLMScan;
        if (wrongInstance || wrongChild)
            throw new OptimizerException("Cannot apply SingleConditionPushdown to query: " + query);
    }

    private IAlgebraOperator toOptimizedScan(IDatabase database, LLMScan scan) {
        TableAlias tableAlias = scan.getTableAlias();
        ITable table = database.getTable(tableAlias.getTableName());

        String conditionAsString = pushdownCondition.toString();
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
                .build();
        return new LLMScan(scan.getTableAlias(), optimizedExecutor, scan.getAttributesSelect());
    }

    private Select toOptimizedSelect(IDatabase database, LLMScan scan) {
        if (this.operation == null || this.operation.isBlank()) return null;
        TableAlias tableAlias = scan.getTableAlias();
        ITable table = database.getTable(tableAlias.getTableName());
        String replaceAlias = tableAlias.getAlias()+".";
        String cleanOperation = cleanExpression(operation);
        String conditionsAsString = remainingConditions.stream()
                .map(Object::toString)
                .map(c->c.replace(replaceAlias, ""))
                .collect(Collectors.joining(cleanOperation));
        Expression expression = new Expression(cleanExpression(conditionsAsString));
        addAllAttributesToExpression(expression, table, tableAlias);
        log.debug("Optimized select expression: {}", expression);
        return new Select(expression);
    }

    private void addAllAttributesToExpression(Expression expression, ITable table, TableAlias tableAlias) {
        // TODO: Add this to utility (same code in FilterParser.java), but pay attention to the variable name in variable description (table alias is missing when parsing using the ParserWhere
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
