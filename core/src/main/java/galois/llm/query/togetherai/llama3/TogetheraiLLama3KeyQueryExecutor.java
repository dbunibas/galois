package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import galois.prompt.EPrompts;
import static galois.utils.FunctionalUtils.orElse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

@Slf4j
@Getter
public class TogetheraiLLama3KeyQueryExecutor extends AbstractKeyBasedQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;

    public TogetheraiLLama3KeyQueryExecutor() {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public TogetheraiLLama3KeyQueryExecutor(
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
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_8B);
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_70B);
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain) {
        Map<String, Object> attributesMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            List<Attribute> currentAttributesList = List.of(attribute);
            Map<String, Object> map = getAttributesValues(table, currentAttributesList, key, chain);
            if (map != null) attributesMap.putAll(map);
        }
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    public static TogetheraiLLama3KeyQueryExecutorBuilder builder() {
        return new TogetheraiLLama3KeyQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLLama3KeyQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLLama3KeyQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }

}
