package galois.optimizer;

import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.IDatabase;

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
