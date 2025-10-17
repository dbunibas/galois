package bsf.optimizer;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public interface IOptimizer {
    IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query);

    default String getName() {
        return this.getClass().getSimpleName();
    }
}
