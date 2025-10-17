package bsf.llm.query.openai;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.Constants;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.llm.query.ISQLExecutor;
import bsf.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import bsf.prompt.EPrompts;

import static bsf.llm.query.ConversationalChainFactory.buildOpenAIConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildOpenAIConversationalRetrievalChain;

// This class extends TogetheraiLlama3SQLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class OpenAISQLQueryExecutor extends TogetheraiLlama3SQLQueryExecutor implements ISQLExecutor {
    public OpenAISQLQueryExecutor(String sql) {
        super(sql);
    }

    public OpenAISQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, sql, contentRetriever);
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (getContentRetriever() == null) {
            return buildOpenAIConversationalChain(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        } else {
            return buildOpenAIConversationalRetrievalChain(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME, getContentRetriever());
        }
    }

    // HACK: This is renamed due to conflicts in the return type with its supertype
    public static OpenAISQLQueryExecutorBuilder newBuilder() {
        return new OpenAISQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class OpenAISQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String sql;

        public OpenAISQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OpenAISQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql,
                    getContentRetriever()
            );
        }
    }
}
