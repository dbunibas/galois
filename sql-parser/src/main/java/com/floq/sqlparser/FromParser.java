package com.floq.sqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import engine.model.database.TableAlias;

public class FromParser extends FromItemVisitorAdapter<TableAlias> {
    @Override
    public <S> TableAlias visit(Table table, S context) {
        String tableName = table.getName();
        Alias alias = table.getAlias();
        String tableAlias = alias == null ? "" : alias.getName();
        return new TableAlias(tableName, tableAlias);
    }
}
