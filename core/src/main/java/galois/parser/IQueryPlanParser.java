package galois.parser;

import galois.llm.algebra.config.OperatorsConfiguration;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

public interface IQueryPlanParser<T> {
    IAlgebraOperator parse(T queryPlan, IDatabase database, OperatorsConfiguration configuration, String sql);
}
