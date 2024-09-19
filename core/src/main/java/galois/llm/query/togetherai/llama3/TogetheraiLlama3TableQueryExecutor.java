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
import speedy.model.expressions.Expression;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3TableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;

    public TogetheraiLlama3TableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public TogetheraiLlama3TableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        this.expression = expression;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_8B);
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_1_8B);
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_70B);
    }

    public static TogetheraiLlama3TableQueryExecutorBuilder builder() {
        return new TogetheraiLlama3TableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3TableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3TableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
