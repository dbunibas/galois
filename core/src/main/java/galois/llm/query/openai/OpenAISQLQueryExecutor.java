package galois.llm.query.openai;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.ISQLExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import galois.prompt.EPrompts;
import galois.utils.Configuration;

import static galois.llm.query.ConversationalChainFactory.buildOpenAIConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildOpenAIConversationalRetrievalChain;

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
            return buildOpenAIConversationalChain(Configuration.getInstance().getOpenaiApiKey(), Configuration.getInstance().getOpenaiModelName());
        } else {
            return buildOpenAIConversationalRetrievalChain(Configuration.getInstance().getOpenaiApiKey(), Configuration.getInstance().getOpenaiModelName(), getContentRetriever());
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
