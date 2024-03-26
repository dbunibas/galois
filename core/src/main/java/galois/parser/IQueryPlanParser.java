package galois.parser;

import galois.llm.algebra.config.OperatorsConfiguration;
import speedy.model.algebra.IAlgebraOperator;

public interface IQueryPlanParser<T> {
    IAlgebraOperator parse(T queryPlan, OperatorsConfiguration configuration);
}
