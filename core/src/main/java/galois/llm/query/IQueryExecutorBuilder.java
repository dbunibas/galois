package galois.llm.query;

import galois.prompt.EPrompts;
import speedy.model.expressions.Expression;

public interface IQueryExecutorBuilder {
    IQueryExecutorBuilder firstPrompt(EPrompts firstPrompt);

    IQueryExecutorBuilder iterativePrompt(EPrompts iterativePrompt);

    IQueryExecutorBuilder attributesPrompt(EPrompts attributesPrompt);

    IQueryExecutorBuilder maxIterations(int maxIterations);

    IQueryExecutorBuilder expression(Expression expression);

    IQueryExecutor build();
}
