package galois.llm.query.ollama.phi3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.*;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.expressions.Expression;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildOllamaPhi3ConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
@Setter
public class OllamaPhi3NLQueryExecutor extends AbstractEntityQueryExecutor implements INLQueryExectutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private String naturalLanguagePrompt;

    public OllamaPhi3NLQueryExecutor(String naturalLanguagePrompt) {
        this.firstPrompt = EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    public OllamaPhi3NLQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            String naturalLanguagePrompt
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.NATURAL_LANGUAGE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        if (naturalLanguagePrompt == null || naturalLanguagePrompt.isBlank())
            throw new IllegalArgumentException("naturalLanguagePrompt cannot be null or blank!");
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    @Override
    public ContentRetriever getContentRetriever() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        return buildOllamaPhi3ConversationalChain();
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return firstPrompt.generateUsingNL(naturalLanguagePrompt, jsonSchema);
    }

    public static OllamaPhi3NLQueryExecutorBuilder builder() {
        return new OllamaPhi3NLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaPhi3NLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String naturalLanguagePrompt;

        public OllamaPhi3NLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaPhi3NLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt
            );
        }
    }
}
