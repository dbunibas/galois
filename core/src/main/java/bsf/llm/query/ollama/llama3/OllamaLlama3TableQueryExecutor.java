package bsf.llm.query.ollama.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.llm.query.AbstractEntityQueryExecutor;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.expressions.Expression;

import static bsf.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildOllamaLlama3ConversationalRetrivalChain;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3TableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public OllamaLlama3TableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public OllamaLlama3TableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression
    ) {
        this(firstPrompt, iterativePrompt, maxIterations, expression, null);
    }

    public OllamaLlama3TableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        this.expression = expression;
        this.contentRetriever = contentRetriever;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if(contentRetriever == null) {
            return buildOllamaLlama3ConversationalChain();
        }else {
            return buildOllamaLlama3ConversationalRetrivalChain(contentRetriever);
        }
    }

    public static OllamaLlama3TableQueryExecutorBuilder builder() {
        return new OllamaLlama3TableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLlama3TableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private ContentRetriever contentRetriever;

        public OllamaLlama3TableQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever) {
            this.contentRetriever = contentRetriever;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3TableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
