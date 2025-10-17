package bsf.llm.query.gemini;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.Constants;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.ConversationalChainFactory;
import bsf.llm.query.ConversationalRetrievalChainFactory;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.llm.query.ISQLExecutor;
import bsf.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import bsf.prompt.EPrompts;


// This class extends TogetheraiLlama3SQLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class GeminiSQLQueryExecutor extends TogetheraiLlama3SQLQueryExecutor implements ISQLExecutor {
    public GeminiSQLQueryExecutor(String sql) {
        super(sql);
    }

    public GeminiSQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, sql, contentRetriever);
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (getContentRetriever() == null) {
            return ConversationalChainFactory.buildGeminiConversationalChain(Constants.GEMINI_API_KEY, Constants.GEMINI_CHAT_MODEL_NAME);
        } else {
            return ConversationalRetrievalChainFactory.buildGeminiConversationalRetrievalChain(Constants.GEMINI_API_KEY, Constants.GEMINI_CHAT_MODEL_NAME, getContentRetriever());
        }
    }

    // HACK: This is renamed due to conflicts in the return type with its supertype
    public static GeminiSQLQueryExecutorBuilder newBuilder() {
        return new GeminiSQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class GeminiSQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String sql;

        public GeminiSQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new GeminiSQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql,
                    getContentRetriever()
            );
        }
    }
}
