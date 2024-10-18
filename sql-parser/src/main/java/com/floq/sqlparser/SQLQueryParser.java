package com.floq.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Scan;

@Slf4j
public class SQLQueryParser {
    public IAlgebraOperator parse(String sql) {
        return parse(sql, (table, ignored) -> new Scan(table));
    }

    public IAlgebraOperator parse(String sql, ScanNodeFactory scanNodeFactory) {
        try {
            log.debug("Parsing sql query {}", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("The only allowed root statement is select!");
            }
            SelectParser selectParser = new SelectParser(scanNodeFactory);
            return ((Select) statement).accept(selectParser, null);
        } catch (JSQLParserException ex) {
            throw new ParserException("Cannot parse sql statement!", ex);
        }
    }
}
