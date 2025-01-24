package floq.optimizer.optimizations;

import com.floq.sqlparser.ParseContext;
import com.floq.sqlparser.WhereParser;
import floq.llm.algebra.LLMScan;
import floq.llm.query.IQueryExecutor;
import floq.optimizer.IOptimization;
import floq.optimizer.OptimizerException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Select;
import engine.model.database.*;
import engine.model.expressions.Expression;

import java.util.List;
import java.util.stream.Collectors;

import static floq.optimizer.PromptOptimizer.optimizePrompt;
import java.util.ArrayList;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

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
        if (wrongInstance || wrongChild) throw new OptimizerException("Cannot apply SingleConditionPushdown to query: " + query);
    }

    private IAlgebraOperator toOptimizedScan(IDatabase database, LLMScan scan) {
        TableAlias tableAlias = scan.getTableAlias();
        ITable table = database.getTable(tableAlias.getTableName());

        String conditionAsString = pushdownCondition.toString();
        // TODO: Add this to utility (same code in FilterParser.java)
        log.debug("Condition as String: " + conditionAsString);
        List<net.sf.jsqlparser.expression.Expression> pushdownList = new ArrayList<>();
        pushdownList.add(pushdownCondition);
        List<Expression> expressions = toEngineExpression(pushdownList, tableAlias);
        Expression pushdownExpression = expressions.get(0);
//        Expression pushdownExpression = new Expression(cleanExpression(conditionAsString));
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
        return new LLMScan(scan.getTableAlias(), optimizedExecutor, scan.getAttributesSelect(), scan.getNormalizationStrategy(), scan.getLlmProbThreshold());
    }

    private Select toOptimizedSelect(IDatabase database, LLMScan scan) {
        if (this.operation == null || this.operation.isBlank()) {
            return null;
        }
        TableAlias tableAlias = scan.getTableAlias();
        ITable table = database.getTable(tableAlias.getTableName());
        String replaceAlias = (tableAlias.isAliased() ? tableAlias.getAlias() : tableAlias.getTableName()) + ".";
        String cleanOperation = cleanExpression(operation);
        log.debug("Clean Operation: {}", cleanOperation);
        List<Expression> remainingConditionsEngine = toEngineExpression(remainingConditions, tableAlias);
        log.debug("Remaining Conditions: {}", remainingConditionsEngine);
        String conditionsAsString = remainingConditionsEngine.stream()
                .map(Object::toString)
                .map(c -> c.replace(replaceAlias, ""))
                .map(c -> c.replace(table.getName() + "_", ""))
                .collect(Collectors.joining(cleanOperation));
        log.debug("Condition as string: {}", conditionsAsString);
//        Expression expression = new Expression(cleanExpression(conditionsAsString));
        if (conditionsAsString.contains("UMinus")) {
            conditionsAsString = conditionsAsString.replace("UMinus(", "-").replaceFirst("[)]", "");
            log.debug("Condition as string: {}", conditionsAsString);
        }
        Expression expression = new Expression(conditionsAsString);
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

    private List<Expression> toEngineExpression(List<net.sf.jsqlparser.expression.Expression> remainingConditions, TableAlias tableAlias) {
        List<Expression> engineExpressions = new ArrayList<>();
        for (net.sf.jsqlparser.expression.Expression remainingCondition : remainingConditions) {
            WhereParser parser = new WhereParser();
            ExpressionList<net.sf.jsqlparser.expression.Expression> el = new ExpressionList<>(List.of(remainingCondition));
            log.debug("Expression: " + remainingCondition);
            ParseContext context = new ParseContext();
            context.addTableAlias(tableAlias);
            WhereParser.WhereParseResult whereResult = parser.visit(el, context);
            log.debug("Parsed expression: " + whereResult.expression());
            engineExpressions.add(whereResult.expression());
        }
        return engineExpressions;
    }
}
