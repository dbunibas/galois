package bsf.llm.query.ollama.phi3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import bsf.llm.query.AbstractEntityQueryExecutor;
import bsf.llm.query.AbstractQueryExecutorBuilder;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.IQueryExecutorBuilder;
import bsf.prompt.EPrompts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.ITable;

import java.util.List;

import static bsf.llm.query.ConversationalChainFactory.buildOllamaPhi3ConversationalChain;
import static bsf.utils.FunctionalUtils.orElse;
import queryexecutor.model.expressions.Expression;

@Slf4j
@Getter
public class OllamaPhi3SQLQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;

    public OllamaPhi3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.sql = sql;
    }

    public OllamaPhi3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("sql cannot be null or blank!");
        this.sql = sql;
    }

    @Override
    public ContentRetriever getContentRetriever() {
        throw new UnsupportedOperationException("The query executor is currently unsupported");
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        return buildOllamaPhi3ConversationalChain();
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression,  String jsonSchema) {
        return firstPrompt.generateUsingSQL(sql, jsonSchema);
    }

    public static OllamaPhi3SQLQueryExecutorBuilder builder() {
        return new OllamaPhi3SQLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class OllamaPhi3SQLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        private String sql;

        public OllamaPhi3SQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new OllamaPhi3SQLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    sql
            );
        }
    }
}
