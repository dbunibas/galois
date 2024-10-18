package floq.llm.query;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import floq.prompt.EPrompts;
import lombok.Getter;
import engine.model.expressions.Expression;

@Getter
public abstract class AbstractQueryExecutorBuilder implements IQueryExecutorBuilder {
    private EPrompts firstPrompt;
    private EPrompts iterativePrompt;
    private EPrompts attributesPrompt;
    private int maxIterations = 10;
    private Expression expression = null;
    private ContentRetriever contentRetriever;

    @Override
    public IQueryExecutorBuilder firstPrompt(EPrompts firstPrompt) {
        this.firstPrompt = firstPrompt;
        return this;
    }

    @Override
    public IQueryExecutorBuilder iterativePrompt(EPrompts iterativePrompt) {
        this.iterativePrompt = iterativePrompt;
        return this;
    }

    @Override
    public IQueryExecutorBuilder attributesPrompt(EPrompts attributesPrompt) {
        this.attributesPrompt = attributesPrompt;
        return this;
    }

    @Override
    public IQueryExecutorBuilder maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    @Override
    public IQueryExecutorBuilder expression(Expression expression) {
        this.expression = expression;
        return this;
    }

    @Override
    public IQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
        return this;
    }
}
