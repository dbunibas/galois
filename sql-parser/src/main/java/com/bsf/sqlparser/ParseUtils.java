package com.bsf.sqlparser;

public class ParseUtils {
    public static <S> ParseContext contextToParseContext(S context) {
        if (!(context instanceof ParseContext)) {
            throw new IllegalArgumentException(String.format("Cannot cast context %s to ParseContext!", context.getClass()));
        }

        return (ParseContext) context;
    }
}
