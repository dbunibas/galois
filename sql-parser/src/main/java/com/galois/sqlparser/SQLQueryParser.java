package com.galois.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Scan;

@Slf4j
public class SQLQueryParser {
    public IAlgebraOperator parse(String sql) {
        return parse(sql, (table, ignored) -> new Scan(table), null);
    }

    public IAlgebraOperator parse(String sql, ScanNodeFactory scanNodeFactory) {
        return parse(sql, scanNodeFactory, null);
    }

    public IAlgebraOperator parse(String sql, IUserDefinedFunctionFactory userDefinedFunctionFactory) {
        return parse(sql, (table, ignored) -> new Scan(table), userDefinedFunctionFactory);
    }

    public IAlgebraOperator parse(String sql, ScanNodeFactory scanNodeFactory, IUserDefinedFunctionFactory userDefinedFunctionFactory) {
        try {
            log.debug("Parsing sql query {}", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("The only allowed root statement is select!");
            }
            SelectParser selectParser = new SelectParser(scanNodeFactory, userDefinedFunctionFactory);
            return ((Select) statement).accept(selectParser, null);
        } catch (JSQLParserException ex) {
            throw new ParserException("Cannot parse sql statement!", ex);
        }
    }
}
