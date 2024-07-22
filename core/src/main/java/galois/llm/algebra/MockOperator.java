package galois.llm.algebra;

import java.util.List;
import speedy.model.algebra.AbstractOperator;
import speedy.model.algebra.operators.IAlgebraTreeVisitor;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;

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
