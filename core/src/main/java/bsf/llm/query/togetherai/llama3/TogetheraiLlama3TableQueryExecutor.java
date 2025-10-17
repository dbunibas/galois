package bsf.llm.query.togetherai.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.Constants;
import bsf.llm.query.AbstractEntityQueryExecutor;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.expressions.Expression;

import static bsf.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildTogetherAIConversationalRetrivalChain;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3TableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public TogetheraiLlama3TableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public TogetheraiLlama3TableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression, ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        this.expression = expression;
        this.contentRetriever = contentRetriever;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if(contentRetriever == null) {
            return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
        }else {
            return buildTogetherAIConversationalRetrivalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL, contentRetriever);
        }
    }

    @Override
    public boolean ensureKeyInAttributes() {
        return true;
    }

    public static TogetheraiLlama3TableQueryExecutorBuilder builder() {
        return new TogetheraiLlama3TableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3TableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3TableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression(),
                    getContentRetriever()
            );
        }
    }
}
