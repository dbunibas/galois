package floq.llm.query.ollama.phi3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import floq.llm.query.AbstractKeyBasedQueryExecutor;
import floq.llm.query.AbstractQueryExecutorBuilder;
import floq.llm.query.IQueryExecutor;
import floq.llm.query.IQueryExecutorBuilder;
import floq.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import engine.model.database.Attribute;
import engine.model.database.ITable;
import engine.model.database.TableAlias;
import engine.model.database.Tuple;
import engine.model.expressions.Expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static floq.llm.query.ConversationalChainFactory.buildOllamaPhi3ConversationalChain;
import static floq.llm.query.ConversationalChainFactory.buildOllamaPhi3ChatLanguageModel;
import static floq.llm.query.utils.QueryUtils.mapToTuple;
import static floq.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaPhi3KeyQueryExecutor extends AbstractKeyBasedQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;

    public OllamaPhi3KeyQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public OllamaPhi3KeyQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            EPrompts attributesPrompt,
            int maxIterations,
            Expression expression
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.LIST_KEY_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_MORE_NO_REPEAT);
        this.attributesPrompt = orElse(attributesPrompt, EPrompts.ATTRIBUTES_JSON);
        this.maxIterations = maxIterations;
        this.expression = expression;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        return buildOllamaPhi3ConversationalChain();
    }

    @Override
    protected ChatLanguageModel getChatLanguageModel() {
        return buildOllamaPhi3ChatLanguageModel();
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, Chain<String, String> chain) {
        Map<String, Object> attributesMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            List<Attribute> currentAttributesList = List.of(attribute);
            Map<String, Object> map = getAttributesValues(table, currentAttributesList, key, chain);
            attributesMap.putAll(map);
        }
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    @Override
    public ContentRetriever getContentRetriever() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    public static OllamaPhi3KeyQueryExecutorBuilder builder() {
        return new OllamaPhi3KeyQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaPhi3KeyQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        @Override
        public IQueryExecutor build() {
            return new OllamaPhi3KeyQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}