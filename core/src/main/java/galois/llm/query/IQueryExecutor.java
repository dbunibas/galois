package galois.llm.query;

import galois.prompt.EPrompts;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.List;

public interface IQueryExecutor {
    List<Tuple> execute(IDatabase database, TableAlias tableAlias);

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
