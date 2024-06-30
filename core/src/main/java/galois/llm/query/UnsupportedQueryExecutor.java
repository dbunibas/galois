package galois.llm.query;

import galois.prompt.EPrompts;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.List;
import speedy.model.database.AttributeRef;

// TODO: Delete class (and restore inheriting children)
public class UnsupportedQueryExecutor implements IQueryExecutor {
    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }
    
    @Override
    public EPrompts getFirstPrompt() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public EPrompts getIterativePrompt() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public int getMaxIterations() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public IQueryExecutorBuilder toBuilder() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }
}
