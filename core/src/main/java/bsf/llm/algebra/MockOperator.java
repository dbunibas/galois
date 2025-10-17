package bsf.llm.algebra;

import java.util.List;
import queryexecutor.model.algebra.AbstractOperator;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;

public class MockOperator extends AbstractOperator {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        return children.get(0).execute(source, target);
    }

    @Override
    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.children.get(0).getAttributes(source, target);
    }

    @Override
    public void accept(IAlgebraTreeVisitor iatv) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
