package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.llm.query.ConversationalChainFactory.buildTogetherAiChatLanguageModel;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3KeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;

    public TogetheraiLlama3KeyScanQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public TogetheraiLlama3KeyScanQueryExecutor(
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
    protected ConversationalChain getConversationalChain() {
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
    }

    @Override
    protected ChatLanguageModel getChatLanguageModel() {
        return buildTogetherAiChatLanguageModel(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain) {
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
                    getExpression()
            );
        }
    }
}
