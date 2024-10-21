package engine.model.algebra.operators;

import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public interface IDelete {

    boolean execute(String tableName, IAlgebraOperator sourceQuery, IDatabase source, IDatabase target);

}
