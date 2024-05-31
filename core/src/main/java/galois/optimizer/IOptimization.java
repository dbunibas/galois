package galois.optimizer;

import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

public interface IOptimization {
    IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query);
}
