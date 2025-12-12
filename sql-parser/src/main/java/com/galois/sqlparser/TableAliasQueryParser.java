package com.galois.sqlparser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import speedy.model.database.TableAlias;

@Slf4j
public class TableAliasQueryParser {
    @Getter
    private ParseContext context;

    public TableAlias parse(String sql) {
        try {
            log.debug("Parsing sql query {} for table alias", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("The only allowed root statement is select!");
            }

            context = new ParseContext();
            TableAliasParser tableAliasParser = new TableAliasParser();
            return ((Select) statement).accept(tableAliasParser, context);
        } catch (JSQLParserException ex) {
            throw new ParserException("Cannot parse sql statement!", ex);
        }
    }

    private static class TableAliasParser extends SelectVisitorAdapter<TableAlias> {
        @Override
        public <S> TableAlias visit(PlainSelect plainSelect, S context) {
            assert context instanceof ParseContext;
            ParseContext parseContext = (ParseContext) context;

            // From
            FromParser fromParser = new FromParser();
            FromItem fromItem = plainSelect.getFromItem();
            TableAlias tableAlias = fromItem.accept(fromParser, parseContext);
            parseContext.addTableAlias(tableAlias);

            // HACK: keeps track of multiple tables when ignoreTree is true in the query executor
            if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
                if (plainSelect.getJoins().size() > 1) {
                    throw new UnsupportedOperationException("Join with more than two tables are unsupported!");
                }
                net.sf.jsqlparser.statement.select.Join firstJoin = plainSelect.getJoins().getFirst();
                TableAlias joinRightTableAlias = firstJoin.getFromItem().accept(fromParser, parseContext);
                parseContext.addTableAlias(joinRightTableAlias);
            }

            return tableAlias;
        }
    }
}
