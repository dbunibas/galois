package bsf.llm.query.togetherai.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.Constants;
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

import static bsf.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static bsf.llm.query.ConversationalChainFactory.buildTogetherAiChatLanguageModel;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildTogetherAIConversationalRetrivalChain;
import static bsf.llm.query.utils.QueryUtils.mapToTuple;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3KeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    public TogetheraiLlama3KeyScanQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public TogetheraiLlama3KeyScanQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            EPrompts attributesPrompt,
            int maxIterations,
            Expression expression, ContentRetriever contentRetriever
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
        if(contentRetriever == null) {
            return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
        }else {
            return buildTogetherAIConversationalRetrivalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL, contentRetriever);
        }
    }

    @Override
    protected ChatLanguageModel getChatLanguageModel() {
        return buildTogetherAiChatLanguageModel(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, Chain<String, String> chain) {
        Map<String, Object> attributesMap = getAttributesValues(table, attributes, key, chain);
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    public static TogetheraiLLama3KeyScanQueryExecutorBuilder builder() {
        return new TogetheraiLLama3KeyScanQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLLama3KeyScanQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3KeyScanQueryExecutor(
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
