package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3NLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String naturalLanguagePrompt;

    public OllamaLlama3NLQueryExecutor(String naturalLanguagePrompt) {
        this.firstPrompt = EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    public OllamaLlama3NLQueryExecutor(
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
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
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

        public OllamaLlama3NLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3NLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt
            );
        }
    }
}