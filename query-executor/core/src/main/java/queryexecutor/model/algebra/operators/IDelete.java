package queryexecutor.model.algebra.operators;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public interface IDelete {

    boolean execute(String tableName, IAlgebraOperator sourceQuery, IDatabase source, IDatabase target);

}
