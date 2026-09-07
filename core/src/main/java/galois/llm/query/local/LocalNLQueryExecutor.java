package galois.llm.query.local;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import galois.prompt.EPrompts;
import galois.utils.Configuration;

import static galois.llm.query.ConversationalChainFactory.buildLocalConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildLocalConversationalRetrievalChain;

// This class extends TogetheraiLlama3NLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class LocalNLQueryExecutor extends TogetheraiLlama3NLQueryExecutor implements INLQueryExectutor {

    public LocalNLQueryExecutor(String naturalLanguagePrompt) {
        super(naturalLanguagePrompt);
    }

    public LocalNLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String naturalLanguagePrompt, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, naturalLanguagePrompt, contentRetriever);
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
    public static LocalNLQueryExecutorBuilder newBuilder() {
        return new LocalNLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class LocalNLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String naturalLanguagePrompt;

        public LocalNLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new LocalNLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt,
                    getContentRetriever()
            );
        }
    }
}
