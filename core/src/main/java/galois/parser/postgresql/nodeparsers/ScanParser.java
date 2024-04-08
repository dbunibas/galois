package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;

public class ScanParser extends AbstractNodeParser {
    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);
//        IAlgebraOperator scan = new Scan(tableAlias);
        IAlgebraOperator scan = new LLMScan(tableAlias, configuration.getScan().getQueryExecutor());

        ProjectParser projectParser = new ProjectParser();
        Element output = node.getChild("Output", node.getNamespace());
        ITable table = database.getTable(tableName);
        if (projectParser.shouldParseNode(output, table)) {
            IAlgebraOperator select = projectParser.parse(node, database, configuration);
            select.addChild(scan);
            return select;
        }

        return scan;
    }
}
