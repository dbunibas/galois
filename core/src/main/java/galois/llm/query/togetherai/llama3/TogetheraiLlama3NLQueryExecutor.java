package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
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

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3NLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String naturalLanguagePrompt;

    public TogetheraiLlama3NLQueryExecutor(String naturalLanguagePrompt) {
        this.firstPrompt = EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    public TogetheraiLlama3NLQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            String naturalLanguagePrompt
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.NATURAL_LANGUAGE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        if (naturalLanguagePrompt == null || naturalLanguagePrompt.isBlank()) {
            throw new IllegalArgumentException("naturalLanguagePrompt cannot be null or blank!");
        }
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_8B);
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_70B);
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return firstPrompt.generateUsingNL(naturalLanguagePrompt, jsonSchema);
    }

    public static TogetheraiLlama3NLQueryExecutorBuilder builder() {
        return new TogetheraiLlama3NLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3NLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private String naturalLanguagePrompt;

        public TogetheraiLlama3NLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3NLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt
            );
        }
    }
}
