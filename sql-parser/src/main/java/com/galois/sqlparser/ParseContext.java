package com.galois.sqlparser;

import lombok.Data;
import net.sf.jsqlparser.schema.Column;
import speedy.model.database.TableAlias;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParseContext {
    // List of table aliases: the first one is the base table alias
    private final List<TableAlias> tableAliases = new ArrayList<>();

    public void addTableAlias(TableAlias tableAlias) {
        tableAliases.add(tableAlias);
    }

    public TableAlias getFromItemTableAlias() {
        return tableAliases.getFirst();
    }

    public TableAlias getTableAliasFromColumn(Column column) {
        if (column.getTable() == null) {
            return getFromItemTableAlias();
        }

        String columnName = column.getTable().getName();
        return tableAliases.stream()
                .filter(t -> columnName.equals(t.getTableName()) || columnName.equals(t.getAlias()))
                .findFirst()
                .orElseThrow();
    }
}
