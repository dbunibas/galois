package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.prompt.EPrompts;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;

@Slf4j
@Getter
public class OllamaLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;

    @Builder
    OllamaLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this.firstPrompt = firstPrompt != null ? firstPrompt : EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = iterativePrompt != null ? iterativePrompt : EPrompts.LIST_MORE_NO_REPEAT;
        this.maxIterations = maxIterations != null ? maxIterations : 5;
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("sql cannot be null or blank!");
        this.sql = sql;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildOllamaLlama3ConversationalChain();
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return firstPrompt.generateUsingSQL(sql, jsonSchema);
    }

    @Override
    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return iterativePrompt.generate();
    }
}
