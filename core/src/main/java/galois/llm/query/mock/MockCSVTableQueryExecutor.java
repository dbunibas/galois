package galois.llm.query.mock;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.AbstractQueryExecutorBuilder;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.prompt.EPrompts;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;
import speedy.model.expressions.Expression;
import speedy.persistence.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.mapToTuple;
import static galois.utils.FunctionalUtils.orElse;

@Slf4j
@Getter
public class MockCSVTableQueryExecutor implements IQueryExecutor {
    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private final Expression expression;
    private final ContentRetriever contentRetriever;

    private List<AttributeRef> attributes = null;

    public MockCSVTableQueryExecutor() {
        this.firstPrompt = EPrompts.FROM_TABLE_CSV;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_CSV;
        this.maxIterations = 10;
        this.expression = null;
        this.contentRetriever = null;
    }

    public MockCSVTableQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            Expression expression,
            ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.FROM_TABLE_CSV);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_CSV);
        this.maxIterations = maxIterations;
        this.expression = expression;
        this.contentRetriever = contentRetriever;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias, Double llmProbThreshold) {
        log.debug("-- Mock execution");
        log.debug("firstPrompt: {}", firstPrompt);
        log.debug("iterativePrompt: {}", iterativePrompt);
        log.debug("attributes: {}", attributes);
        log.debug("expression: {}", expression);
        log.debug("maxIteration: {}", maxIterations);
        log.debug("contentRetriever: {}", contentRetriever);
        log.debug("----");

        ITable table = database.getTable(tableAlias.getTableName());
        List<Attribute> attributesExecution = new ArrayList<>();
        for (AttributeRef aRef : attributes) {
            // Guardrail for existing attributes
            Attribute attribute = table.getAttributes().stream()
                    .filter(a -> a.getName().equalsIgnoreCase(aRef.getName()))
                    .findFirst()
                    .orElse(null);
            if (attribute == null) {
                continue;
            }

            attributesExecution.add(table.getAttribute(attribute.getName()));
        }

        StringBuilder sb = new StringBuilder();

        // header
        for (int i = 0; i < attributesExecution.size(); i++) {
            Attribute attribute = attributesExecution.get(i);
            sb.append(attribute.getName());
            if (i < attributesExecution.size() - 1) sb.append(",");
        }
        sb.append("\n");

        // values

        int maxSize = 5;
        for (int i = 0; i < maxSize; i++) {
            for (int j = 0; j < attributesExecution.size(); j++) {
                Attribute attribute = attributesExecution.get(j);
                sb.append(attribute.getType().equals(Types.STRING) ? "abc" : 0.0);
                if (j < attributesExecution.size() - 1) sb.append(",");
            }
            if (i < maxSize - 1) sb.append("\n");
        }

        String stringResponse = sb.toString();
        List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(stringResponse, table);

        List<Tuple> results = new ArrayList<>();
        for (Map<String, Object> map : parsedResponse) {
            Tuple tuple = mapToTuple(map, tableAlias, attributesExecution);
            results.add(tuple);
        }

        log.info("scan results: {}", results);
        return results;
    }

    @Override
    public void setAttributes(List<AttributeRef> attributes) {
        this.attributes = attributes;
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return new MockQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder toBuilder() {
        return new MockQueryExecutorBuilder();
    }

    @AllArgsConstructor
    private static class MockQueryExecutorBuilder extends AbstractQueryExecutorBuilder {
        @Override
        public IQueryExecutor build() {
            return new MockCSVTableQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    getExpression(),
                    getContentRetriever()
            );
        }
    }
}
