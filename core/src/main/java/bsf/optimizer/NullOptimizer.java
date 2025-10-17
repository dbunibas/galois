package bsf.optimizer;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public class NullOptimizer implements IOptimizer{

    @Override
    public IAlgebraOperator optimize(IDatabase database, String sql, IAlgebraOperator query) {
        // do nothing
        return query;
    }

    @Override
    public String getName() {
        return "NullOptimizer";
    }
    
}
