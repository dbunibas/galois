package galois.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.prompt.EPrompts;
import galois.utils.attributes.AttributesOverride;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.List;

// TODO: Delete class (and restore inheriting children)
public class UnsupportedQueryExecutor implements IQueryExecutor {
    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias, Double llmProbThreshold) {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public void setAttributesOverride(AttributesOverride attributesOverride) {
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
