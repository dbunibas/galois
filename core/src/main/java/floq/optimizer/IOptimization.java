package floq.optimizer;

import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public interface IOptimization {
    IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query);
}
