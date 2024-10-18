package floq.parser;

import floq.llm.algebra.config.OperatorsConfiguration;
import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public interface IQueryPlanParser<T> {
    IAlgebraOperator parse(T queryPlan, IDatabase database, OperatorsConfiguration configuration, String sql);
}
