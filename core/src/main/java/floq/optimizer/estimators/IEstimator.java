package floq.optimizer.estimators;

import engine.model.database.ITable;
import engine.model.expressions.Expression;

public interface IEstimator {
    double estimate(ITable table);

    double estimateWithExpression(ITable table, Expression expression);
}
