package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.algebra.Select;
import speedy.model.database.Attribute;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ScanParser extends AbstractNodeParser {

    private String conditionNode = "Filter";

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);
        ProjectParser projectParser = new ProjectParser();
        FilterParser filterParser = new FilterParser(conditionNode);
        ITable table = database.getTable(tableName);
        Element output = node.getChild("Output", node.getNamespace());
        IAlgebraOperator root = null;
        IAlgebraOperator select = null;
        IAlgebraOperator filter = null;
        if (projectParser.shouldParseNode(output, table)) {
            select = projectParser.parse(node, database, configuration);
            List<ProjectionAttribute> projectionAttributes = projectParser.getProjectionAttributes(output);
            List<AttributeRef> attributes = new ArrayList<>();
            for (ProjectionAttribute pa : projectionAttributes) {
                if (!attributes.contains(pa.getAttributeRef())) {
                    attributes.add(pa.getAttributeRef());
                }
            }
            if (node.getChild(conditionNode, node.getNamespace()) != null) {
                filter = filterParser.parse(node, database, configuration);
                Select s = (Select) filter;
                List<Expression> selections = s.getSelections();
                for (Expression selection : selections) {
                    List<String> variables = selection.getVariables();
                    for (String variable : variables) {
                        // Handle null (IS NULL / IS NOT NULL expressions)
                        if (variable.equalsIgnoreCase("null")) continue;
                        StringTokenizer tokenizer = new StringTokenizer(variable, ".");
                        tokenizer.nextToken();
                        String attributeName = tokenizer.nextToken();
                        AttributeRef aRef = new AttributeRef(tableName, attributeName);
                        if (!containsAttributeByName(attributes, aRef)) {
                            attributes.add(aRef);
                        }
//                        if (!attributes.contains(aRef)) attributes.add(aRef);
                    }
                }
            }
            log.info("Creating LLM Scan with: " + attributes.toString());
            root = new LLMScan(tableAlias, configuration.getScan().getQueryExecutor(), attributes);
        } else {
            log.info("Creating LLM Scan for the table");
            if (node.getChild(conditionNode, node.getNamespace()) != null) {
                filter = filterParser.parse(node, database, configuration);
            }
            root = new LLMScan(tableAlias, configuration.getScan().getQueryExecutor());
        }
        if (filter != null && node.getChild(conditionNode, node.getNamespace()) != null) {
            filter.addChild(root);
            root = filter;
        }
        if (select != null && projectParser.shouldParseNode(output, table)) {
            select.addChild(root);
            root = select;
        }
        return root;
    }


}
