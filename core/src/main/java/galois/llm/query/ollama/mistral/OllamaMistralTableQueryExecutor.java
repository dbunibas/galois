package galois.llm.query.ollama.mistral;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.expressions.Expression;

import static galois.llm.query.ConversationalChainFactory.buildOllamaMistralConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaMistralTableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;

    public OllamaMistralTableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.expression = null;
    }

    public OllamaMistralTableQueryExecutor(
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
    protected Chain<String, String> getConversationalChain() {
        return buildOllamaMistralConversationalChain();
    }

    @Override
    public ContentRetriever getContentRetriever() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    public static OllamaMistralTableQueryExecutorBuilder builder() {
        return new OllamaMistralTableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaMistralTableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new OllamaMistralTableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
