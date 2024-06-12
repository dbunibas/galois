package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.ConversationalChain;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
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

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class TogetheraiLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;

    public TogetheraiLlama3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.sql = sql;
    }

    public TogetheraiLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql cannot be null or blank!");
        }
        this.sql = sql;
    }

    @Override
    protected ConversationalChain getConversationalChain() {
        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_8B);
//        return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, TogetherAIModel.MODEL_LLAMA3_70B);
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return firstPrompt.generateUsingSQL(sql, jsonSchema);
    }

    public static TogetheraiLlama3SQLQueryExecutorBuilder builder() {
        return new TogetheraiLlama3SQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3SQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private String sql;

        public TogetheraiLlama3SQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3SQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql
            );
        }
    }
}
