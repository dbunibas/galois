package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.prompt.EAttributesPrompts;
import galois.prompt.EPrompts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.SpeedyConstants;
import speedy.model.database.*;

import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class OllamaLLama3KeyScanQueryExecutor extends AbstractKeyBasedQueryExecutor {
    @Builder.Default
    private final EPrompts firstPrompt = EPrompts.LIST_KEY_PIPE;
    @Builder.Default
    private final EPrompts iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
    @Builder.Default
    private final EPrompts attributesPrompt = EPrompts.ATTRIBUTES_COMMA;
    @Builder.Default
    private final int maxIterations = 5;

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain) {
        String prompt = attributesPrompt.generate(table, key, attributes);
        log.debug("Attributes prompt is: {}", prompt);

        String response = chain.execute(prompt);
        log.debug("addValueFromAttributes response: {}", response);

        Map<String, Object> attributesMap = attributesPrompt.getAttributesParser().parse(response, attributes);
        for (Attribute attribute : attributes) {
            IValue cellValue = attributesMap.containsKey(attribute.getName()) ?
                    new ConstantValue(attributesMap.get(attribute.getName())) :
                    new NullValue(SpeedyConstants.NULL_VALUE);
            Cell currentCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attribute.getName()),
                    cellValue
            );
            tuple.addCell(currentCell);
        }

        return tuple;
    }
}
