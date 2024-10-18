package floq.optimizer;

import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public interface IOptimizer {
    IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query);

    default String getName() {
        return this.getClass().getSimpleName();
    }
}
