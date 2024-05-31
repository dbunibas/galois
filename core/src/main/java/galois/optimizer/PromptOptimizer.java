package galois.optimizer;

import galois.prompt.EPrompts;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PromptOptimizer {
    private static final Map<EPrompts, EPrompts> OPTIMIZATIONS = Map.ofEntries(
            Map.entry(EPrompts.LIST_KEY_JSON, EPrompts.LIST_KEY_JSON_CONDITION)
    );

    public static EPrompts optimizePrompt(EPrompts prompt) {
        if (OPTIMIZATIONS.containsKey(prompt)) return OPTIMIZATIONS.get(prompt);
        log.info("Cannot optimize prompt: {}, returning same value...", prompt);
        return prompt;
    }
}
