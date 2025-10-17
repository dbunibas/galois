package bsf.llm.algebra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import bsf.llm.database.CellWithProb;
import bsf.llm.database.LLMDB;
import bsf.llm.database.LLMTable;
import bsf.llm.models.togetherai.CellProb;
import bsf.llm.models.togetherai.TupleProb;
import bsf.llm.query.IQueryExecutor;
import bsf.llm.query.ISQLExecutor;
import bsf.llm.query.utils.QueryUtils;
import bsf.utils.attributes.AttributesOverride;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.*;
import queryexecutor.persistence.Types;
import queryexecutor.utility.AlgebraUtility;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class LLMScan extends Scan {

    private static final Logger logger = LoggerFactory.getLogger(LLMScan.class);

    private final TableAlias tableAlias;
    private final IQueryExecutor queryExecutor;
    private List<AttributeRef> attributesSelect = null;
    private final String normalizationStrategy;
    private Double llmProbThreshold = null;
    private final boolean checkTypes = true;
    private final boolean removeDuplicates = true;

    @Setter
    private AttributesOverride attributesOverride = null;

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

    public LLMScan(TableAlias tableAlias, IQueryExecutor queryExecutor, List<AttributeRef> attributesSelect, String normalizationStrategy, Double llmProbThreshold) {
        super(tableAlias);
        this.tableAlias = tableAlias;
        this.queryExecutor = queryExecutor;
        this.attributesSelect = attributesSelect;
        this.normalizationStrategy = normalizationStrategy;
        this.llmProbThreshold = llmProbThreshold;
    }

    public LLMScan(TableAlias tableAlias, IQueryExecutor queryExecutor, String normalizationStrategy, Double llmProbThreshold) {
        super(tableAlias);
        this.tableAlias = tableAlias;
        this.queryExecutor = queryExecutor;
        this.normalizationStrategy = normalizationStrategy;
        this.llmProbThreshold = llmProbThreshold;
    }

    @Override
    public String getName() {
        return "SCAN_LLM(" + tableAlias + " - " + attributesSelect + ")";
    }

    @Override
    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitScan(this);
    }

    @Override
    public ITupleIterator execute(IDatabase source, IDatabase target) {
        checkSourceTarget(source, target);
        if (this.attributesSelect != null) {
            return new LLMScanTupleIterator(source, queryExecutor, attributesSelect, attributesOverride);
        }
        return new  LLMScanTupleIterator(source, queryExecutor, attributesOverride);
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
            logger.info("Target database is ignored when using LLM algebra...");
        }

        if (!(source instanceof LLMDB)) {
            throw new IllegalArgumentException("LLM algebra execution is allowed only on LLMDB");
        }
    }

    private class LLMScanTupleIterator implements ITupleIterator {

        private final IDatabase database;
        private final IQueryExecutor queryExecutor;

        public LLMScanTupleIterator(IDatabase database, IQueryExecutor queryExecutor, AttributesOverride attributesOverride) {
            this.database = database;
            this.queryExecutor = queryExecutor;
            if (attributesOverride != null) this.queryExecutor.setAttributesOverride(attributesOverride);
        }

        public LLMScanTupleIterator(IDatabase database, IQueryExecutor queryExecutor, List<AttributeRef> attributesSelect, AttributesOverride attributesOverride) {
            this.database = database;
            this.queryExecutor = queryExecutor;
            this.queryExecutor.setAttributes(attributesSelect);
            if (attributesOverride != null) this.queryExecutor.setAttributesOverride(attributesOverride);
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
            logger.info("LLMScanTupleIterator close is not implemeted yet!");
        }

        @Override
        public boolean hasNext() {
            return currentIndex < currentResult.size() || currentTry < MAX_TRIES;
        }

        @Override
        public Tuple next() {
            if (!currentResult.isEmpty() && currentResult.size() > currentIndex) {
                Tuple result = currentResult.get(currentIndex);
                currentIndex++;
                if (normalizationStrategy != null) {
                    result = QueryUtils.normalizeTextValues(result, normalizationStrategy);
                }
                if (checkTypes) {
                    boolean check = checkTupleTypes(result, database);
                    if (!check && hasNext()) {
                        logger.warn("Ignoring tuple because has different types: " + result);
                        return next();
                    }
                }
                return result;
            }
            currentResult = new ArrayList<>(queryExecutor.execute(database, tableAlias, llmProbThreshold));
//            currentResult = loadStoredTuples();
//            saveTuplesWithProb(currentResult);
            if (llmProbThreshold != null && !(queryExecutor instanceof ISQLExecutor)) filterTuplesWithProb();
            if (removeDuplicates && !(queryExecutor instanceof ISQLExecutor))
                AlgebraUtility.removeTupleDuplicatesIgnoreOID(currentResult);

            currentTry++;

            if (!currentResult.isEmpty()) {
                Tuple result = currentResult.get(0);
                currentIndex++;
                if (normalizationStrategy != null) {
                    result = QueryUtils.normalizeTextValues(result, normalizationStrategy);
                }
                if (checkTypes) {
                    boolean check = checkTupleTypes(result, database);
                    if (!check && hasNext()) {
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
                try {
                    Attribute attribute = table.getAttribute(cell.getAttribute());
                    if (logger.isDebugEnabled())
                        logger.debug("Attribute " + attribute.getName() + " with type " + attribute.getType() + " - value: " + cell.getValue().getPrimitiveValue().toString());
                    if (logger.isDebugEnabled())
                        logger.debug("Numerical Attribute? " + Types.isNumerical(attribute.getType()));
                    if (logger.isDebugEnabled())
                        logger.debug("Type check: " + Types.checkType(attribute.getType(), cell.getValue().getPrimitiveValue().toString()));
                    if (Types.isNumerical(attribute.getType()) && (cell.getValue().getPrimitiveValue().toString().isBlank() || !Types.checkType(attribute.getType(), cell.getValue().getPrimitiveValue().toString()))) {
                        if (attribute.getNullable()) {
                            if (logger.isDebugEnabled()) logger.debug("Attribute is nullable - Set cell to 'null'");
                            cell.setValue(new NullValue("null"));
                        } else {
                            if (logger.isDebugEnabled()) logger.debug("Skip check condition - Return false");
                            return false;
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    if (logger.isDebugEnabled()) logger.debug("Exception: {}", iae);

                    // generated attribute from a function...just skip it
                }
            }
            return true;
        }

        private void filterTuplesWithProb() {
            List<Tuple> tuples = currentResult;
            List<Tuple> filtered = new ArrayList<>();
            for (Tuple tuple : tuples) {
                if (canAddWithProb(tuple, "mean", true)) filtered.add(tuple);
            }
            currentResult = filtered;
        }

        private boolean canAddWithProb(Tuple tuple, String strategy, boolean normalize) {
            List<CellWithProb> cellsWithProb = new ArrayList<>();
            for (Cell cell : tuple.getCells()) {
                CellWithProb cellWithProb = (CellWithProb) cell;
                if (cellWithProb.getCellProb() == null) continue;
                cellsWithProb.add(cellWithProb);
//                log.info("Cell with Prob: {}", cellWithProb.toShortString());
            }
            if (normalize) {
                Double maxProb = findMax(cellsWithProb);
                normalize(cellsWithProb, maxProb);
            }
            if (log.isInfoEnabled()) {
                log.info(printTupleWithProb(tuple));
            }
            if (strategy.equalsIgnoreCase("min")) {
                // strategy all cell at least with the same prob
                for (CellWithProb cellWithProb : cellsWithProb) {
                    if (cellWithProb.getValueProb() < llmProbThreshold) {
                        log.info("Discarded");
                        return false;
                    }
                }
                log.info("Accepted");
                return true;
            }
            if (strategy.equalsIgnoreCase("mean")) {
                // strategy mean 
                double mean = 0.0;
                for (CellWithProb cellWithProb : cellsWithProb) {
                    mean += cellWithProb.getValueProb();
                }
                mean = mean / cellsWithProb.size();
                log.info("Accepted mean? {}", mean >= llmProbThreshold);
                return mean >= llmProbThreshold;
            }
            if (strategy.equals("msp")) {
                double sumLogProb = 0.0;
                for (CellWithProb cellWithProb : cellsWithProb) {
                    sumLogProb += Math.log(cellWithProb.getValueProb());
                }
                double msp = Math.exp(sumLogProb);
                log.info("Accepted MSP? {}", msp >= llmProbThreshold);
                return msp >= llmProbThreshold;
            }
            // default add all
            return true;
        }

        private String printTupleWithProb(Tuple tuple) {
            String toPrint = "";
            for (Cell cell : tuple.getCells()) {
                toPrint += cell.toShortString() + " | ";
            }
            return toPrint;
        }

        private Double findMax(List<CellWithProb> cellsWithProb) {
            Double max = cellsWithProb.getFirst().getValueProb();
            for (CellWithProb cellWithProb : cellsWithProb) {
                if (cellWithProb.getValueProb() > max) {
                    max = cellWithProb.getValueProb();
                }
            }
            return max;
        }

        private void normalize(List<CellWithProb> cellsWithProb, Double maxProb) {
            for (CellWithProb cellWithProb : cellsWithProb) {
                Double newValue = cellWithProb.getValueProb() / maxProb;
                cellWithProb.getCellProb().setValueProb(newValue);
            }
        }

        private void saveTuplesWithProb(List<Tuple> currentResult) {
            // for exp only
            String dbName = "spider-geo";
            String queryN = "Q1";
            String fileName = "TMP-PATH" + dbName + "-" + queryN + ".json";
            List<TupleProb> tuplesProb = new ArrayList<>();
            AlgebraUtility.removeTupleDuplicatesIgnoreOID(currentResult);
            for (Tuple tuple : currentResult) {
                TupleProb tp = toTupleProb(tuple);
                tuplesProb.add(tp);
            }
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            try {
                String json = ow.writeValueAsString(tuplesProb);
                FileUtils.writeStringToFile(new File(fileName), json, Charset.forName("UTF-8"));
            } catch (Exception e) {
                log.error("Unable to save file: {}", fileName);
                log.error("Exception in saving the JSON: {}", e);
            }
        }

        private TupleProb toTupleProb(Tuple tuple) {
            TupleProb tp = new TupleProb();
            for (Cell cell : tuple.getCells()) {
                if (cell instanceof CellWithProb) {
                    CellWithProb cp = (CellWithProb) cell;
                    if (cp.getCellProb() != null) tp.addCell(cp.getCellProb());
                }
            }
            return tp;
        }

        private Tuple toTuple(TupleProb tp, long oidValue) {
            String tableName = tableAlias.getTableName();
//            ITable table = database.getTable(tableName);
            TupleOID oid = new TupleOID(oidValue);
            Tuple t = new Tuple(oid);
            for (CellProb cell : tp.getCells()) {
                String attributeName = cell.getAttributeName();
                Object value = cell.getValue();
                IValue ivalue;
                if (value == null) {
                    ivalue = new NullValue("null");
                } else {
                    ivalue = new ConstantValue(value);
                }
                CellWithProb cwp = new CellWithProb(oid, new AttributeRef(tableAlias, attributeName), ivalue, cell);
                t.addCell(cwp);
            }
            return t;
        }

        private List<Tuple> loadStoredTuples() {
            // for exp only
            String dbName = "spider-geo";
            String queryN = "Q8";
            String fileName = "TMP_PATH" + dbName + "-" + queryN + ".json";
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<List<TupleProb>> listTuple = new TypeReference<>() {
            };
            List<Tuple> tuples = new ArrayList<>();
            try {
                List<TupleProb> tuplesProb = mapper.readValue(new File(fileName), listTuple);
                long oid = 1;
                for (TupleProb tupleProb : tuplesProb) {
                    Tuple t = toTuple(tupleProb, oid);
                    oid++;
                    tuples.add(t);
                }
            } catch (Exception e) {
                log.error("Unable to open file: {}", fileName);
                log.error("Exception in reading obj from JSON: {}", e);
            }
            return tuples;
        }
    }
}
