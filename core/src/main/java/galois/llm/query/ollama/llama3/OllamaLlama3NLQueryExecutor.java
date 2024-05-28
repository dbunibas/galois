package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.prompt.EPrompts;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;

@Slf4j
@Getter
public class OllamaLlama3NLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String naturalLanguagePrompt;

    @Builder
    OllamaLlama3NLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String naturalLanguagePrompt) {
        this.firstPrompt = firstPrompt != null ? firstPrompt : EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = iterativePrompt != null ? iterativePrompt : EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = maxIterations != null ? maxIterations : 5;
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

    @Override
    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return iterativePrompt.generate();
    }
}
