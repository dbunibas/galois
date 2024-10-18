package floq.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import floq.prompt.EPrompts;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;
import engine.model.database.Tuple;

import java.util.List;
import engine.model.database.AttributeRef;

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

    @Override
    public ContentRetriever getContentRetriever() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }
}
