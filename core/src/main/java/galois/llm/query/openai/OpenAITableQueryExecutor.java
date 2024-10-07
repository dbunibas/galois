package galois.llm.query.openai;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.Constants;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.expressions.Expression;

import static galois.llm.query.ConversationalChainFactory.buildOpenAIConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildOpenAIConversationalRetrievalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OpenAITableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public OpenAITableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public OpenAITableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        this.expression = expression;
        this.contentRetriever = contentRetriever;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (contentRetriever == null) {
            return buildOpenAIConversationalChain(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        } else {
            return buildOpenAIConversationalRetrievalChain(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME, contentRetriever);
        }
    }

    public static OpenAITableQueryExecutorBuilder builder() {
        return new OpenAITableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OpenAITableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new OpenAITableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression(),
                    getContentRetriever()
            );
        }
    }
}
