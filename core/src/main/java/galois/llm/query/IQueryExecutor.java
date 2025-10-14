package galois.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.prompt.EPrompts;
import galois.utils.attributes.AttributesOverride;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.List;

public interface IQueryExecutor {
    List<Tuple> execute(IDatabase database, TableAlias tableAlias, Double llmProbThreshold);

    void setAttributes(List<AttributeRef> attributes);

    /**
     * Set the override attributes for the current executor.
     * If the executor supports the fetched tuples will contain all the attributes
     * (even if those are not included in the actual table).
     *
     * @param attributesOverride the *full* list of attributes to retrieve
     */
    default void setAttributesOverride(AttributesOverride attributesOverride) {
        throw new UnsupportedOperationException("setOverrideAttributes needs to be implemented for the current executor!");
    }

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
