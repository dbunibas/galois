package galois.llm.query.local;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import galois.utils.Configuration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.expressions.Expression;

import static galois.llm.query.ConversationalChainFactory.buildLocalConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildLocalConversationalRetrievalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class LocalCSVTableQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public LocalCSVTableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_CSV;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_CSV;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public LocalCSVTableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_CSV);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_CSV);
        this.maxIterations = maxIterations;
        this.expression = expression;
        this.contentRetriever = contentRetriever;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (contentRetriever == null) {
            return buildLocalConversationalChain(Configuration.getInstance().getLocalBaseUrl(), Configuration.getInstance().getLocalModelName());
        } else {
            return buildLocalConversationalRetrievalChain(Configuration.getInstance().getLocalBaseUrl(), Configuration.getInstance().getLocalModelName(), contentRetriever);
        }
    }

    @Override
    public boolean ensureKeyInAttributes() {
        return true;
    }

    public static LocalCSVTableQueryExecutorBuilder builder() {
        return new LocalCSVTableQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class LocalCSVTableQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new LocalCSVTableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression(),
                    getContentRetriever()
            );
        }
    }
}
