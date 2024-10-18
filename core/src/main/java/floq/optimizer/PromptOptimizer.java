package floq.optimizer;

import floq.prompt.EPrompts;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PromptOptimizer {
    private static final Map<EPrompts, EPrompts> OPTIMIZATIONS = Map.ofEntries(
            Map.entry(EPrompts.LIST_KEY_JSON, EPrompts.LIST_KEY_JSON_CONDITION),
            Map.entry(EPrompts.LIST_KEY_PIPE, EPrompts.LIST_KEY_PIPE_CONDITION),
            Map.entry(EPrompts.FROM_TABLE_JSON, EPrompts.FROM_TABLE_JSON_CONDITION),
            Map.entry(EPrompts.LIST_KEY_COMMA, EPrompts.LIST_KEY_COMMA_CONDITION)
    );

    public static EPrompts optimizePrompt(EPrompts prompt) {
        if (prompt == null || !OPTIMIZATIONS.containsKey(prompt)) {
            log.info("Cannot optimize prompt: {}, returning same value...", prompt);
            return prompt;
        }
        return OPTIMIZATIONS.get(prompt);
    }
}
