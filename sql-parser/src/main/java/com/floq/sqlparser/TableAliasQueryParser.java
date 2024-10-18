package com.floq.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import engine.model.database.TableAlias;

@Slf4j
public class TableAliasQueryParser {
    public TableAlias parse(String sql) {
        try {
            log.debug("Parsing sql query {} for table alias", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("The only allowed root statement is select!");
            }
            TableAliasParser tableAliasParser = new TableAliasParser();
            return ((Select) statement).accept(tableAliasParser, null);
        } catch (JSQLParserException ex) {
            throw new ParserException("Cannot parse sql statement!", ex);
        }
    }

    private static class TableAliasParser extends SelectVisitorAdapter<TableAlias> {
        @Override
        public <S> TableAlias visit(PlainSelect plainSelect, S context) {
            FromParser fromParser = new FromParser();
            FromItem fromItem = plainSelect.getFromItem();
            return fromItem.accept(fromParser, null);
        }
    }
}
