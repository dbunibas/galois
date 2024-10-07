package galois.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.prompt.EPrompts;
import speedy.model.expressions.Expression;

public interface IQueryExecutorBuilder {
    IQueryExecutorBuilder firstPrompt(EPrompts firstPrompt);

    IQueryExecutorBuilder iterativePrompt(EPrompts iterativePrompt);

    IQueryExecutorBuilder attributesPrompt(EPrompts attributesPrompt);

    IQueryExecutorBuilder maxIterations(int maxIterations);

    IQueryExecutorBuilder expression(Expression expression);

    IQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever);

    IQueryExecutor build();
}
