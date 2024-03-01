package galois.parser.postgresql.nodeparsers;

import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Scan;
import speedy.model.database.TableAlias;

public class ScanParser implements INodeParser {
    @Override
    public IAlgebraOperator parse(Element node) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        return new Scan(tableAlias);
    }
}
