package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.LLMScan;
import galois.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Scan;
import speedy.model.database.TableAlias;

public class ScanParser extends AbstractNodeParser {
    @Override
    public IAlgebraOperator parse(Element node, OperatorsConfiguration configuration) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);
        return new LLMScan(tableAlias, configuration.getScan().getQueryExecutor());
//        return new Scan(tableAlias);
    }
}
