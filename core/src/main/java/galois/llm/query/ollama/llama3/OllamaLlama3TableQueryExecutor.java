package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.prompt.EPrompts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class OllamaLlama3TableQueryExecutor extends AbstractEntityQueryExecutor {
    @Builder.Default
    private final EPrompts firstPrompt = EPrompts.FROM_TABLE_JSON;
    @Builder.Default
    private final EPrompts iterativePrompt = EPrompts.LIST_MORE_NO_REPEAT;
    @Builder.Default
    private final int maxIterations = 5;

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }
}
