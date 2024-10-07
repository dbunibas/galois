package galois.llm.query.openai;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.Constants;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import galois.prompt.EPrompts;

import static galois.llm.query.ConversationalChainFactory.buildOpenAIConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildOpenAIConversationalRetrievalChain;

// This class extends TogetheraiLlama3NLQueryExecutor in order to inherit the exact execute method used in previous experiments
// Also, ignoreTree and generateFirstPrompt would be identical to super, so those aren't overridden
public class OpenAINLQueryExecutor extends TogetheraiLlama3NLQueryExecutor implements INLQueryExectutor {

    public OpenAINLQueryExecutor(String naturalLanguagePrompt) {
        super(naturalLanguagePrompt);
    }

    public OpenAINLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String naturalLanguagePrompt, ContentRetriever contentRetriever) {
        super(firstPrompt, iterativePrompt, maxIterations, naturalLanguagePrompt, contentRetriever);
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
    public static OpenAINLQueryExecutorBuilder newBuilder() {
        return new OpenAINLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return newBuilder();
    }

    public static class OpenAINLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String naturalLanguagePrompt;

        public OpenAINLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OpenAINLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt,
                    getContentRetriever()
            );
        }
    }
}
