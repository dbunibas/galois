package galois.llm.query.ollama.mistral;

import dev.langchain4j.chain.ConversationalChain;
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

import static galois.llm.query.ConversationalChainFactory.buildOllamaMistralConversationalChain;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaFromAttributes;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaMistralKeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;

    public OllamaMistralKeyScanQueryExecutor(
    ) {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public OllamaMistralKeyScanQueryExecutor(
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
        return buildOllamaMistralConversationalChain();
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain) {
        String jsonSchema = generateJsonSchemaFromAttributes(table, attributes);
        String prompt = attributesPrompt.generate(table, key, attributes, jsonSchema);
        log.debug("Attributes prompt is: {}", prompt);
        String response = chain.execute(prompt);
        log.debug("addValueFromAttributes response: {}", response);
        Map<String, Object> attributesMap = attributesPrompt.getAttributesParser().parse(response, attributes);
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }

    public static OllamaMistralKeyScanQueryExecutorBuilder builder() {
        return new OllamaMistralKeyScanQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaMistralKeyScanQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new OllamaMistralKeyScanQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
