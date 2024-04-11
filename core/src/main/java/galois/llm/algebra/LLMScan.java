package galois.llm.algebra;

import galois.llm.database.LLMDB;
import galois.llm.database.LLMTable;
import galois.llm.query.IQueryExecutor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.algebra.AbstractOperator;
import speedy.model.algebra.operators.IAlgebraTreeVisitor;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LLMScan extends AbstractOperator {

    private static final Logger logger = LoggerFactory.getLogger(LLMScan.class);

    @Getter
    private final TableAlias tableAlias;
    private final IQueryExecutor queryExecutor;

    public LLMScan(TableAlias tableAlias, IQueryExecutor queryExecutor) {
        this.tableAlias = tableAlias;
        this.queryExecutor = queryExecutor;
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
        return new LLMScanTupleIterator(table, queryExecutor);
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
        private final IQueryExecutor queryExecutor;

        public LLMScanTupleIterator(LLMTable table, IQueryExecutor queryExecutor) {
            this.table = table;
            this.queryExecutor = queryExecutor;
        }

        private static final int MAX_TRIES = 1;

        private int currentTry = 0;
        private List<Tuple> currentResult = new ArrayList<>();
        private int currentIndex = 0;

        @Override
        public void reset() {
            currentIndex = 0;
        }

        @Override
        public void close() {
            logger.warn("LLMScanTupleIterator close is not implemeted yet!");
        }

        @Override
        public boolean hasNext() {
            return currentIndex < currentResult.size() || currentTry < MAX_TRIES;
        }

        @Override
        public Tuple next() {
            if (!currentResult.isEmpty()) {
                Tuple result = currentResult.get(currentIndex);
                currentIndex++;
                return result;
            }

            currentResult = queryExecutor.execute(table, tableAlias);
            currentTry++;

            Tuple result = currentResult.get(0);
            currentIndex++;
            return result;
        }
    }
}
