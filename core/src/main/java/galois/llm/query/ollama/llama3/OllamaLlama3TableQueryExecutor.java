package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.expressions.Expression;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3TableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;

    public OllamaLlama3TableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.expression = null;
    }

    public OllamaLlama3TableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        this.expression = expression;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }

    public static OllamaLlama3TableQueryExecutorBuilder builder() {
        return new OllamaLlama3TableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLlama3TableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3TableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
