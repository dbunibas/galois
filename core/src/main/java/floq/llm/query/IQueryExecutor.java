package floq.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import floq.prompt.EPrompts;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.TableAlias;
import engine.model.database.Tuple;
import engine.model.expressions.Expression;

import java.util.List;

public interface IQueryExecutor {
    List<Tuple> execute(IDatabase database, TableAlias tableAlias);

    void setAttributes(List<AttributeRef> attributes);

    EPrompts getFirstPrompt();

    EPrompts getIterativePrompt();

    int getMaxIterations();

    default EPrompts getAttributesPrompt() {
        return null;
    }

    default Expression getExpression() {
        return null;
    }

    default boolean ignoreTree() {
        return false;
    }

    default boolean ensureKeyInAttributes() {
        return false;
    }

    ContentRetriever getContentRetriever();

    IQueryExecutorBuilder getBuilder();

    default IQueryExecutorBuilder toBuilder() {
        return getBuilder()
                .firstPrompt(getFirstPrompt())
                .iterativePrompt(getIterativePrompt())
                .attributesPrompt(getAttributesPrompt())
                .maxIterations(getMaxIterations())
                .expression(getExpression());
    }
}
