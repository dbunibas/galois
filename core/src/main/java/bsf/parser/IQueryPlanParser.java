package bsf.parser;

import bsf.llm.algebra.config.OperatorsConfiguration;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public interface IQueryPlanParser<T> {
    IAlgebraOperator parse(T queryPlan, IDatabase database, OperatorsConfiguration configuration, String sql);
}
