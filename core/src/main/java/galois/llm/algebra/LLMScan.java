package galois.llm.algebra;

import galois.llm.database.LLMDB;
import galois.llm.database.LLMTable;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.OllamaMistralQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.algebra.AbstractOperator;
import speedy.model.algebra.operators.IAlgebraTreeVisitor;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LLMScan extends AbstractOperator {

    private static final Logger logger = LoggerFactory.getLogger(LLMScan.class);

    private final TableAlias tableAlias;

    public LLMScan(TableAlias tableAlias) {
        this.tableAlias = tableAlias;
    }

    @Override
    public String getName() {
        return "SCAN_LLM(" + tableAlias + ")";
    }

    @Override
    public void accept(IAlgebraTreeVisitor visitor) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        checkSourceTarget(source, target);
        LLMTable table = (LLMTable) source.getTable(tableAlias.getTableName());
        return new LLMScanTupleIterator(table);
    }

    @Override
    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        checkSourceTarget(source, target);
        LLMTable table = (LLMTable) source.getTable(tableAlias.getTableName());
        return table.getAttributes().stream()
                .map(attr -> new AttributeRef(tableAlias, attr.getName()))
                .collect(Collectors.toUnmodifiableSet())
                .stream()
                .toList();
    }

    private void checkSourceTarget(IDatabase source, IDatabase target) {
        if (target != null) logger.warn("Target database is ignored when using LLM algebra...");

        if (!(source instanceof LLMDB))
            throw new IllegalArgumentException("LLM algebra execution is allowed only on LLMDB");
    }

    private class LLMScanTupleIterator implements ITupleIterator {

        private final LLMTable table;

        public LLMScanTupleIterator(LLMTable table) {
            this.table = table;
        }

        private static final int MAX_TRIES = 2;

        private final IQueryExecutor queryExecutor = new OllamaMistralQueryExecutor();

        private int currentTry = 0;
        private List<Tuple> currentResult = new ArrayList<>();

        @Override
        public void reset() {
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public void close() {
            logger.warn("LLMScanTupleIterator close is not implemeted yet!");
        }

        @Override
        public boolean hasNext() {
            return !currentResult.isEmpty() || currentTry < MAX_TRIES;
        }

        @Override
        public Tuple next() {
            if (!currentResult.isEmpty()) return currentResult.remove(0);

            String prompt = currentTry == 0 ? getInitialPrompt() : getNextPrompt();
            currentResult = queryExecutor.execute(prompt, table);
            currentTry++;

            return currentResult.remove(0);
        }

        private String getInitialPrompt() {
            // TODO: Implement
            StringBuilder prompt = new StringBuilder();
            prompt.append("List some ").append(table.getName()).append("s. ");
            prompt.append("For each of them return ");
            String attributes = table.getAttributes().stream()
                    .map(Attribute::getName)
                    .filter(name -> !name.equals("oid"))
                    .collect(Collectors.joining("|"));
            prompt.append(attributes);
            return prompt.toString();
        }

        private String getNextPrompt() {
            // TODO: Implement
            StringBuilder prompt = new StringBuilder();
            prompt.append("List some other ").append(table.getName()).append("s. ");
            prompt.append("For each of them return ");
            String attributes = table.getAttributes().stream()
                    .map(Attribute::getName)
                    .filter(name -> !name.equals("oid"))
                    .collect(Collectors.joining("|"));
            prompt.append(attributes);
            return prompt.toString();
        }


    }
}
