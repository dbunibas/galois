package galois.llm.query.local;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import galois.utils.Configuration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildLocalChatLanguageModel;
import static galois.llm.query.ConversationalChainFactory.buildLocalConversationalChain;
import static galois.llm.query.ConversationalRetrievalChainFactory.buildLocalConversationalRetrievalChain;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class LocalKeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public LocalKeyScanQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public LocalKeyScanQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            EPrompts attributesPrompt,
            int maxIterations,
            Expression expression,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.LIST_KEY_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_MORE_NO_REPEAT);
        this.attributesPrompt = orElse(attributesPrompt, EPrompts.ATTRIBUTES_JSON);
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
    protected ChatLanguageModel getChatLanguageModel() {
        return buildLocalChatLanguageModel(Configuration.getInstance().getLocalBaseUrl(), Configuration.getInstance().getLocalModelName());
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, Chain<String, String> chain) {
        Map<String, Object> attributesMap = getAttributesValues(table, attributes, key, chain);
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    public static LocalKeyScanQueryExecutorBuilder builder() {
        return new LocalKeyScanQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class LocalKeyScanQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        @Override
        public IQueryExecutor build() {
            return new LocalKeyScanQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression(),
                    getContentRetriever()
            );
        }
    }

}
