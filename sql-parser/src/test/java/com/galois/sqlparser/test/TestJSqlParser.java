package com.galois.sqlparser.test;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestJSqlParser {
    @Test
    public void testParseSelectFrom() throws JSQLParserException {
        String sql = "select * from tableName";

        Statement parse = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(PlainSelect.class, parse);
        PlainSelect plainSelect = (PlainSelect) parse;

        // select
        assertEquals(1, plainSelect.getSelectItems().size());
        assertEquals("*", plainSelect.getSelectItems().getFirst().toString());
        // from
        assertNotNull(plainSelect.getFromItem());
        assertEquals("tableName", plainSelect.getFromItem().toString());
        // where
        assertNull(plainSelect.getWhere());
    }
}
