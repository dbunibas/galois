package galois.llm.query.ollama.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.AbstractEntityQueryExecutor;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;

    public OllamaLlama3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.sql = sql;
    }

    OllamaLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
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

    public static OllamaLlama3SQLQueryExecutorBuilder builder() {
        return new OllamaLlama3SQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaLlama3SQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String sql;

        public OllamaLlama3SQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaLlama3SQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql
            );
        }
    }
}
