package galois.optimizer.estimators;

import speedy.model.database.ITable;
import speedy.model.expressions.Expression;

public interface IEstimator {
    double estimate(ITable table);

    double estimateWithExpression(ITable table, Expression expression);
}
