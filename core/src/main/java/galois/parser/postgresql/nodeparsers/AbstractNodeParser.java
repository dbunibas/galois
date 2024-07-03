package galois.parser.postgresql.nodeparsers;

import galois.parser.ParserException;
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
}
