package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractKeyBasedQueryExecutor;
import galois.prompt.EPrompts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static galois.llm.query.utils.QueryUtils.mapToTuple;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class OllamaLLama3KeyQueryExecutor extends AbstractKeyBasedQueryExecutor {
    @Builder.Default
    private final EPrompts firstPrompt = EPrompts.LIST_KEY_PIPE;
    @Builder.Default
    private final EPrompts iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
    @Builder.Default
    private final EPrompts attributesPrompt = EPrompts.ATTRIBUTES_COMMA;
    @Builder.Default
    private final int maxIterations = 1;

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }

    @Override
    protected Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain) {
        Map<String, Object> attributesMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            String prompt = attributesPrompt.generate(table, key, List.of(attribute));
            log.debug("Attribute prompt is: {}", prompt);
            String response = chain.execute(prompt);
            log.debug("Attribute response is: {}", response);
            attributesMap.put(attribute.getName(), response);
        }
        return mapToTuple(tuple, attributesMap, tableAlias, attributes);
    }
}
