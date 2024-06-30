package galois.llm.query;

import galois.prompt.EPrompts;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.List;
import speedy.model.database.AttributeRef;

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
