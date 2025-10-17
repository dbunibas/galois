package queryexecutor.model.algebra.udf;

import queryexecutor.model.algebra.operators.ITupleIterator;

public interface IUserDefinedFunction {

    // TODO: add databases?
    ITupleIterator execute(ITupleIterator iterator);

}
