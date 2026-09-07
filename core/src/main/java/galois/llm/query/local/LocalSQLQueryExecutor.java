package galois.llm.query.local;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.ISQLExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3SQLQueryExecutor;
import galois.prompt.EPrompts;
import galois.utils.Configuration;

import static galois.llm.query.ConversationalChainFactory.buildLocalConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildLocalConversationalRetrievalChain;

// This class extends TogetheraiLlama3SQLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class LocalSQLQueryExecutor extends TogetheraiLlama3SQLQueryExecutor implements ISQLExecutor {
    public LocalSQLQueryExecutor(String sql) {
        super(sql);
    }

    public LocalSQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, sql, contentRetriever);
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (getContentRetriever() == null) {
            return buildLocalConversationalChain(Configuration.getInstance().getLocalBaseUrl(), Configuration.getInstance().getLocalModelName());
        } else {
            return buildLocalConversationalRetrievalChain(Configuration.getInstance().getLocalBaseUrl(), Configuration.getInstance().getLocalModelName(), getContentRetriever());
        }
    }

    // HACK: This is renamed due to conflicts in the return type with its supertype
    public static LocalSQLQueryExecutorBuilder newBuilder() {
        return new LocalSQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class LocalSQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String sql;

        public LocalSQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new LocalSQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql,
                    getContentRetriever()
            );
        }
    }
}
