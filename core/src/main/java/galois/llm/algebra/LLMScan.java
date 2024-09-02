package galois.llm.algebra;

import galois.llm.database.LLMDB;
import galois.llm.database.LLMTable;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.utils.QueryUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.algebra.Scan;
import speedy.model.algebra.operators.IAlgebraTreeVisitor;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import speedy.model.database.Attribute;
import speedy.model.database.Cell;
import speedy.model.database.ITable;
import speedy.model.database.NullValue;
import speedy.persistence.Types;

@Getter
public class LLMScan extends Scan {

    private static final Logger logger = LoggerFactory.getLogger(LLMScan.class);
    
    private final TableAlias tableAlias;
    private final IQueryExecutor queryExecutor;
    private List<AttributeRef> attributesSelect = null;
    private final String normalizationStrategy;
    private final boolean checkTypes = true;

    public LLMScan(TableAlias tableAlias, IQueryExecutor queryExecutor, String normalizationStrategy) {
        super(tableAlias);
        this.tableAlias = tableAlias;
        this.queryExecutor = queryExecutor;
        this.normalizationStrategy = normalizationStrategy;
    }

    public LLMScan(TableAlias tableAlias, IQueryExecutor queryExecutor, List<AttributeRef> attributesSelect, String normalizationStrategy) {
        super(tableAlias);
        this.tableAlias = tableAlias;
        this.queryExecutor = queryExecutor;
        this.attributesSelect = attributesSelect;
        this.normalizationStrategy = normalizationStrategy;
    }

    @Override
    public String getName() {
        return "SCAN_LLM(" + tableAlias + ")";
    }

    @Override
    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitScan(this);
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        checkSourceTarget(source, target);
        if (this.attributesSelect != null) {
            return new LLMScanTupleIterator(source, queryExecutor, attributesSelect);
        }
        return new LLMScanTupleIterator(source, queryExecutor);
    }

    @Override
    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        checkSourceTarget(source, target);

        LLMTable table = (LLMTable) source.getTable(tableAlias.getTableName());
        Set<AttributeRef> tableAttributes = table.getAttributes().stream()
                .map(attr -> new AttributeRef(tableAlias, attr.getName()))
                .collect(Collectors.toUnmodifiableSet());

        if (attributesSelect != null) {
            // This filters eventual attributes with aliases
            return this.attributesSelect.stream()
                    .filter(tableAttributes::contains)
                    .toList();
        }

        return tableAttributes.stream().toList();
    }

    public void setAttributesSelect(List<AttributeRef> attributesSelect) {
        this.attributesSelect = attributesSelect;
    }

    private void checkSourceTarget(IDatabase source, IDatabase target) {
        // TODO: Switch source and target?
        if (target != null) {
            logger.warn("Target database is ignored when using LLM algebra...");
        }

        if (!(source instanceof LLMDB)) {
            throw new IllegalArgumentException("LLM algebra execution is allowed only on LLMDB");
        }
    }

    private class LLMScanTupleIterator implements ITupleIterator {

        private final IDatabase database;
        private final IQueryExecutor queryExecutor;

        public LLMScanTupleIterator(IDatabase database, IQueryExecutor queryExecutor) {
            this.database = database;
            this.queryExecutor = queryExecutor;
        }

        public LLMScanTupleIterator(IDatabase database, IQueryExecutor queryExecutor, List<AttributeRef> attributesSelect) {
            this.database = database;
            this.queryExecutor = queryExecutor;
            this.queryExecutor.setAttributes(attributesSelect);
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
                if (normalizationStrategy != null) {
                    result = QueryUtils.normalizeTextValues(result, normalizationStrategy);
                }
                if (checkTypes) {
                    boolean check = checkTupleTypes(result, database);
                    if (!check)  {
                        logger.warn("Ignoring tuple because has different types: " + result);
                        return next();
                    }
                }
                return result;
            }

            currentResult = queryExecutor.execute(database, tableAlias);
            currentTry++;

            if (!currentResult.isEmpty()) {
                Tuple result = currentResult.get(0);
                currentIndex++;
                if (normalizationStrategy != null) {
                    result = QueryUtils.normalizeTextValues(result, normalizationStrategy);
                }
                if (checkTypes) {
                    boolean check = checkTupleTypes(result, database);
                    if (!check)  {
                        logger.warn("Ignoring tuple because has different types: " + result);
                        return next();
                    }
                }
                return result;
            }
            return null;
        }

        private boolean checkTupleTypes(Tuple tuple, IDatabase database) {
            ITable table = database.getTable(tableAlias.getTableName());
            for (Cell cell : tuple.getCells()) {
                Attribute attribute = table.getAttribute(cell.getAttribute());
                if (logger.isDebugEnabled()) logger.debug("Attribute " + attribute.getName() + " with type " + attribute.getType() + " - value: " + cell.getValue().getPrimitiveValue().toString());
                if (logger.isDebugEnabled()) logger.debug("Numerical Attribute? " + Types.isNumerical(attribute.getType()));
                if (logger.isDebugEnabled()) logger.debug("Type check: " + Types.checkType(attribute.getType(), cell.getValue().getPrimitiveValue().toString()));
                if (Types.isNumerical(attribute.getType()) && (cell.getValue().getPrimitiveValue().toString().isBlank() || !Types.checkType(attribute.getType(), cell.getValue().getPrimitiveValue().toString()))) {
                    if (attribute.getNullable()) {
                        if (logger.isDebugEnabled()) logger.debug("Attribute is nullable - Set cell to 'null'");
                        cell.setValue(new NullValue("null"));
                    } else {
                        if (logger.isDebugEnabled()) logger.debug("Skip check condition - Return false");
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
