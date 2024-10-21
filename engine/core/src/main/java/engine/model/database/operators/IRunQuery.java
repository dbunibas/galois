package engine.model.database.operators;

import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.IDatabase;
import engine.model.database.ResultInfo;

public interface IRunQuery {

    ITupleIterator run(IAlgebraOperator query, IDatabase source, IDatabase target);

    ResultInfo getSize(IAlgebraOperator query, IDatabase source, IDatabase target);
    
    public boolean isUseTrigger();

}
