package galois.parser.postgresql.nodeparsers;

import galois.parser.ParserException;
import java.util.Collection;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;

public abstract class AbstractNodeParser implements INodeParser {

    private TableAlias tableAlias;

    @Override
    public TableAlias getTableAlias() {
        if (tableAlias == null) {
            throw new ParserException("tableAlias is null! It should have been initialized during the parsing...");
        }
        return tableAlias;
    }

    protected void setTableAlias(TableAlias tableAlias) {
        this.tableAlias = tableAlias;
    }

    protected boolean containsAttributeByName(Collection<AttributeRef> attributes, AttributeRef aRef) {
        for (AttributeRef attribute : attributes) {
            if (attribute.getName().equals(aRef.getName())) {
                return true;
            }
        }
        return false;
    }
}
