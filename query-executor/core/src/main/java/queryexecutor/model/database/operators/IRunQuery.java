package queryexecutor.model.database.operators;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ResultInfo;

public interface IRunQuery {

    ITupleIterator run(IAlgebraOperator query, IDatabase source, IDatabase target);

    ResultInfo getSize(IAlgebraOperator query, IDatabase source, IDatabase target);
    
    public boolean isUseTrigger();

}
