package engine.model.database.operators;

import engine.model.algebra.IAlgebraOperator;
import engine.model.database.IDatabase;

public interface IExplainQuery {

    public long explain(IAlgebraOperator query, IDatabase source, IDatabase target) ;

}
