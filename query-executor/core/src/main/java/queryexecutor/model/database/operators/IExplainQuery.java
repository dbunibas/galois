package queryexecutor.model.database.operators;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.IDatabase;

public interface IExplainQuery {

    public long explain(IAlgebraOperator query, IDatabase source, IDatabase target) ;

}
