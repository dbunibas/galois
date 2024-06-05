package galois.optimizer;

import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

public interface IOptimizer {
    IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query);
}
