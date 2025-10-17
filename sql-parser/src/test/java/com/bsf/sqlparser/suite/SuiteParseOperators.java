package com.bsf.sqlparser.suite;

import com.bsf.sqlparser.test.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        TestParseSelect.class,
        TestParseWhere.class,
        TestParseOrderBy.class,
        TestParseGroupBy.class,
        TestParseJoin.class,
        TestComplexWhere.class,
})
public class SuiteParseOperators {
}
