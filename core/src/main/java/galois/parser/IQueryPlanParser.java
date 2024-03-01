package galois.parser;

import speedy.model.algebra.IAlgebraOperator;

public interface IQueryPlanParser<T> {
    IAlgebraOperator parse(T queryPlan);
}
