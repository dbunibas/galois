package com.galois.sqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import speedy.model.algebra.udf.IUserDefinedFunction;

@FunctionalInterface
public interface IUserDefinedFunctionFactory {
    IUserDefinedFunction getUserDefinedFunction(String name, ExpressionList<? extends Expression> expressions, ParseContext parseContext);
}
