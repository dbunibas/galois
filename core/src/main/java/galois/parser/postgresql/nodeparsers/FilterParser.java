package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.ParserException;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class FilterParser extends AbstractNodeParser {
    private static final List<String> SYMBOLS = List.of(
            ">=",
            "<=",
            ">",
            "<",
            "="
    );

    private static final Map<String, String> SYMBOL_MAP = Map.ofEntries(
            Map.entry(">=", ">="),
            Map.entry("<=", "<="),
            Map.entry(">", ">"),
            Map.entry("<", "<"),
            Map.entry("=", "==")
    );

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        // TODO: Add to AbstractNodeParser?
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);

        String filterText = node.getChild("Filter", node.getNamespace()).getText();
        log.debug("filterText: {}", filterText);
        Expression expression = parseExpression(filterText);
        log.debug("Parsed expression: {}", expression);
        return new Select(expression);
    }

    private Expression parseExpression(String text) {
        // TODO: Handle boolean operations. This method can parse a SINGLE expression (ex. a > b)!
        String symbol = SYMBOLS.stream()
                .filter(text::contains)
                .findFirst()
                .orElseThrow(ParserException::new);

        String[] operands = text
                .replaceAll("[()]", "")
                .split(symbol);
        List<String> trimmedOperands = Arrays.stream(operands)
                .map(String::trim)
                .toList();

        String firstOperand = trimmedOperands.get(0).replace(getTableAlias().getAlias() + ".", "");
        String operator = SYMBOL_MAP.get(symbol);
        String secondOperand = parseSecondOperand(trimmedOperands.get(1));

        Expression exp = new Expression("(" + firstOperand + " " + operator + " " + secondOperand + ")");
        exp.setVariableDescription(firstOperand, new AttributeRef(getTableAlias(), firstOperand));
        return exp;
    }

    private String parseSecondOperand(String operand) {
        if (operand.contains("text")) {
            return operand.replace("::text", "").replaceAll("'", "\"");
        }
        return operand;
    }
}
