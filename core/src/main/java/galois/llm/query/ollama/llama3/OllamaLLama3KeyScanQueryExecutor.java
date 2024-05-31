package galois.llm.query.ollama.llama3;

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

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaFromAttributes;
import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLLama3KeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final EPrompts attributesPrompt;
    private final int maxIterations;
    private final Expression expression;

    public OllamaLLama3KeyScanQueryExecutor(
    ) {
        this.firstPrompt = EPrompts.LIST_KEY_JSON;
        this.iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
        this.attributesPrompt = EPrompts.ATTRIBUTES_JSON;
        this.maxIterations = 10;
        this.expression = null;
    }

    public OllamaLLama3KeyScanQueryExecutor(
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
        return buildOllamaLlama3ConversationalChain();
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

    public static OllamaLLama3KeyScanQueryExecutorBuilder builder() {
        return new OllamaLLama3KeyScanQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLLama3KeyScanQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new OllamaLLama3KeyScanQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getAttributesPrompt(),
                    getMaxIterations(),
                    getExpression()
            );
        }
    }
}
