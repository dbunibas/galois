package bsf.optimizer.estimators;

import queryexecutor.model.database.ITable;
import queryexecutor.model.expressions.Expression;

public interface IEstimator {
    double estimate(ITable table);

    double estimateWithExpression(ITable table, Expression expression);
}
