package bsf.llm.query.ollama.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.llm.query.*;
import bsf.prompt.EPrompts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.ITable;
import queryexecutor.model.expressions.Expression;

import java.util.List;

import static bsf.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildOllamaLlama3ConversationalRetrivalChain;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
@Setter
public class OllamaLlama3NLQueryExecutor extends AbstractEntityQueryExecutor implements INLQueryExectutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private String naturalLanguagePrompt;
    private final ContentRetriever contentRetriever;

    public OllamaLlama3NLQueryExecutor(String naturalLanguagePrompt) {
        this.firstPrompt = EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.naturalLanguagePrompt = naturalLanguagePrompt;
        this.contentRetriever = null;
    }

    public OllamaLlama3NLQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            String naturalLanguagePrompt
    ) {
        this(firstPrompt, iterativePrompt, maxIterations, naturalLanguagePrompt, null);
    }

    public OllamaLlama3NLQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            String naturalLanguagePrompt,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.NATURAL_LANGUAGE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        if (naturalLanguagePrompt == null || naturalLanguagePrompt.isBlank())
            throw new IllegalArgumentException("naturalLanguagePrompt cannot be null or blank!");
        this.naturalLanguagePrompt = naturalLanguagePrompt;
        this.contentRetriever = contentRetriever;
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if(contentRetriever == null) {
            return buildOllamaLlama3ConversationalChain();
        }else {
            return buildOllamaLlama3ConversationalRetrivalChain(contentRetriever);
        }
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return firstPrompt.generateUsingNL(naturalLanguagePrompt, jsonSchema);
    }

    public static OllamaLlama3NLQueryExecutorBuilder builder() {
        return new OllamaLlama3NLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLlama3NLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String naturalLanguagePrompt;
        private ContentRetriever contentRetriever;

        public OllamaLlama3NLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        public OllamaLlama3NLQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever) {
            this.contentRetriever = contentRetriever;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3NLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt,
                    contentRetriever
            );
        }
    }
}
