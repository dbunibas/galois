package bsf.optimizer;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public interface IOptimization {
    IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query);
}
