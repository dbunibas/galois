package com.galois.sqlparser;

import lombok.Data;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.TableAlias;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParseContext {
    // List of table aliases: the first one is the base table alias
    private final List<TableAlias> tableAliases = new ArrayList<>();
    private final IUserDefinedFunctionFactory userDefinedFunctionFactory;

    public ParseContext() {
        this.userDefinedFunctionFactory = null;
    }

    public ParseContext(IUserDefinedFunctionFactory userDefinedFunctionFactory) {
        this.userDefinedFunctionFactory = userDefinedFunctionFactory;
    }

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

    public TableAlias getTableAliasFromTable(Table table) {
        return tableAliases.stream()
                .filter(t -> table.getName().equals(t.getAlias()) || table.getName().equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
    }

    public IUserDefinedFunction getUserDefinedFunction(String name, ExpressionList<? extends Expression> expressions) {
        if (userDefinedFunctionFactory == null) {
            throw new ParserException("Cannot return a User Defined Function without a factory!");
        }
        return userDefinedFunctionFactory.getUserDefinedFunction(name, expressions, this);
    }
}
