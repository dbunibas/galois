package bsf.llm.query.ollama.llama3;

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
import queryexecutor.model.expressions.Expression;

import java.util.List;

import static bsf.llm.query.ConversationalChainFactory.buildOllamaLlama3ConversationalChain;
import static bsf.llm.query.ConversationalRetrievalChainFactory.buildOllamaLlama3ConversationalRetrivalChain;
import static bsf.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class OllamaLlama3SQLQueryExecutor extends AbstractEntityQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final String sql;
    private final ContentRetriever contentRetriever;

    public OllamaLlama3SQLQueryExecutor(String sql) {
        this.firstPrompt = EPrompts.FROM_SQL_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES;
        this.maxIterations = 10;
        this.sql = sql;
        this.contentRetriever = null;
    }

    public OllamaLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql) {
        this(firstPrompt, iterativePrompt, maxIterations, sql, null);
    }

    public OllamaLlama3SQLQueryExecutor(EPrompts firstPrompt, EPrompts iterativePrompt, Integer maxIterations, String sql, ContentRetriever contentRetriever) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_SQL_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES);
        this.maxIterations = maxIterations;
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("sql cannot be null or blank!");
        this.sql = sql;
        this.contentRetriever = contentRetriever;
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if (contentRetriever == null) {
            return buildOllamaLlama3ConversationalChain();
        } else {
            return buildOllamaLlama3ConversationalRetrivalChain(contentRetriever);
        }
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
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
        private ContentRetriever contentRetriever;

        public OllamaLlama3SQLQueryExecutorBuilder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public OllamaLlama3SQLQueryExecutorBuilder contentRetriever(ContentRetriever contentRetriever) {
            this.contentRetriever = contentRetriever;
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
