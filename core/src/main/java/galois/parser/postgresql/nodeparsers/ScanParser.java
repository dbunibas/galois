package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;

@NoArgsConstructor
@AllArgsConstructor
public class ScanParser extends AbstractNodeParser {
    private String conditionNode = "Filter";

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);
//        IAlgebraOperator scan = new Scan(tableAlias);
        IAlgebraOperator root = new LLMScan(tableAlias, configuration.getScan().getQueryExecutor());

        if (node.getChild(conditionNode, node.getNamespace()) != null) {
            FilterParser filterParser = new FilterParser(conditionNode);
            IAlgebraOperator filter = filterParser.parse(node, database, configuration);
            filter.addChild(root);
            root = filter;
        }

        ProjectParser projectParser = new ProjectParser();
        Element output = node.getChild("Output", node.getNamespace());
        ITable table = database.getTable(tableName);
        if (projectParser.shouldParseNode(output, table)) {
            IAlgebraOperator select = projectParser.parse(node, database, configuration);
            select.addChild(root);
            root = select;
        }

        return root;
    }
}
