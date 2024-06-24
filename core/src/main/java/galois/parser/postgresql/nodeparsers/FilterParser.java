package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Select;
import speedy.model.database.*;
import speedy.model.expressions.Expression;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class FilterParser extends AbstractNodeParser {
    private String conditionNode = "Filter";

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        // TODO: Add to AbstractNodeParser?
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);

        log.debug("filtering using node: {}", conditionNode);
        String filterText = node.getChild(conditionNode, node.getNamespace()).getText();
        log.debug("filterText: {}", filterText);
        Expression expression = parseExpression(filterText, database);
        log.debug("Parsed expression: {}", expression);
        return new Select(expression);
    }

    private Expression parseExpression(String text, IDatabase database) {
        log.debug("Expression: " + text);
        String cleanText = cleanExpression(text);
        log.debug("Cleaned Expression: " + cleanText);
        Expression expression = new Expression(cleanText);

        ITable table = database.getTable(getTableAlias().getTableName());
        for (Attribute attribute : table.getAttributes()) {
            String variable = getTableAlias().getAlias() + "." + attribute.getName();
            expression.setVariableDescription(variable, new AttributeRef(getTableAlias(), attribute.getName()));
        }

        return expression;
    }

    private String cleanExpression(String text) {
        // TODO: Handle all cases by using a map! (TestParseXML::testWhereWithBooleanOperationsNot should than work)
        return text
                .replaceAll("::text", "")
                .replaceAll("::double precision", "")
                .replaceAll("'", "\"")
                .replaceAll("=", "==")
                .replaceAll("AND", "&&")
                .replaceAll("OR", "||");
    }
}
