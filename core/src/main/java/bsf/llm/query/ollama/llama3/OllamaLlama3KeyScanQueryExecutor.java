package bsf.llm.query.ollama.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.llm.query.AbstractKeyBasedQueryExecutor;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.expressions.Expression;

import java.util.List;
import java.util.Map;

import static bsf.llm.query.ConversationalChainFactory.buildOllamaLlama3ChatLanguageModel;
import static bsf.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildOllamaLlama3ConversationalRetrivalChain;
import static bsf.llm.query.utils.QueryUtils.mapToTuple;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3KeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public OllamaLlama3KeyScanQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public OllamaLlama3KeyScanQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            EPrompts attributesPrompt,
            int maxIterations,
            Expression expression
    ) {
        this(firstPrompt, iterativePrompt, attributesPrompt, maxIterations, expression, null);
    }

    public OllamaLlama3KeyScanQueryExecutor(
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
            return buildOllamaLlama3ConversationalChain();
        } else {
            return buildOllamaLlama3ConversationalRetrivalChain(contentRetriever);
        }
    }

    @Override
    protected ChatLanguageModel getChatLanguageModel() {
        return buildOllamaLlama3ChatLanguageModel();
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, Chain<String, String> chain) {
        Map<String, Object> attributesMap = getAttributesValues(table, attributes, key, chain);
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    public static OllamaLLama3KeyScanQueryExecutorBuilder builder() {
        return new OllamaLLama3KeyScanQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLLama3KeyScanQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private ContentRetriever contentRetriever;

        public OllamaLLama3KeyScanQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever) {
            this.contentRetriever = contentRetriever;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3KeyScanQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
