package engine.model.algebra;

import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.mainmemory.MainMemoryDB;
import engine.model.database.mainmemory.MainMemoryVirtualDB;
import engine.utility.EngineUtility;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.Cell;
import engine.model.database.EmptyDB;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.TableAlias;
import engine.model.database.Tuple;

public class Scan extends AbstractOperator {
    
    private static Logger logger = LoggerFactory.getLogger(Scan.class);

    private TableAlias tableAlias;

    public Scan(TableAlias tableAlias) {
        this.tableAlias = tableAlias;
    }      

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitScan(this);
    }

    public String getName() {
        return "SCAN(" + tableAlias + ")";
    }

    public TableAlias getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(TableAlias tableAlias) {
        this.tableAlias = tableAlias;
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        if (!EngineConstants.DBMS_DEBUG &&
                (!(source == null || source instanceof MainMemoryDB || source instanceof MainMemoryVirtualDB || source instanceof EmptyDB)
                || !(target instanceof MainMemoryDB || target instanceof MainMemoryVirtualDB))) {
            throw new IllegalArgumentException("Algebra execution is allowed only on MainMemoryDB");
        }
        IDatabase database = null;    
        if (tableAlias.isSource()) {
            database = source;
        } else {
            database = target;
        }
        ITable table = database.getTable(tableAlias.getTableName());
        if(table == null){
            throw new IllegalArgumentException("Unable to scan table " + tableAlias.getTableName() + " in " + database);
        }
        return new ScanTupleIterator(table.getTupleIterator());        
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        ITable table = null;
        if (tableAlias.isSource()) {
            table = source.getTable(tableAlias.getTableName());
        } else {
            table = target.getTable(tableAlias.getTableName());            
        }
        List<AttributeRef> result = new ArrayList<AttributeRef>();
        for (Attribute attribute : table.getAttributes()) {
            AttributeRef attributeRef = new AttributeRef(tableAlias, attribute.getName());
//            result.add(attributeRef);
            EngineUtility.addIfNotContained(result, attributeRef);
        }
        return result;
    }
    
    class ScanTupleIterator implements ITupleIterator {

        private ITupleIterator tableIterator;

        public ScanTupleIterator(ITupleIterator tableIterator) {
            this.tableIterator = tableIterator;
        }

        public boolean hasNext() {
            return tableIterator.hasNext();
        }

        public Tuple next() {
            Tuple tuple = tableIterator.next().clone();
            for (Cell cell : tuple.getCells()) {
                cell.setAttributeRef(new AttributeRef(cell.getAttributeRef(), tableAlias));
            }
            if (logger.isDebugEnabled()) logger.debug("Scanning tuple" + tuple);
            return tuple;
        }

        public void reset() {
            this.tableIterator.reset();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

        public void close() {
            tableIterator.close();
        }

    }

}
