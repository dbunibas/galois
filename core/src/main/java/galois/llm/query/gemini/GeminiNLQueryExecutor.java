package galois.llm.query.gemini;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.Constants;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.ConversationalChainFactory;
import galois.llm.query.ConversationalRetrievalChainFactory;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import galois.prompt.EPrompts;

// This class extends TogetheraiLlama3NLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class GeminiNLQueryExecutor extends TogetheraiLlama3NLQueryExecutor implements INLQueryExectutor {

    public GeminiNLQueryExecutor(String naturalLanguagePrompt) {
        super(naturalLanguagePrompt);
    }

    public GeminiNLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String naturalLanguagePrompt, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, naturalLanguagePrompt, contentRetriever);
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
    public static GeminiNLQueryExecutorBuilder newBuilder() {
        return new GeminiNLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class GeminiNLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String naturalLanguagePrompt;

        public GeminiNLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new GeminiNLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt,
                    getContentRetriever()
            );
        }
    }
}
