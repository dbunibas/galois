package queryexecutor.model.algebra;

import queryexecutor.model.algebra.operators.IAlgebraTreeVisitor;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.model.database.mainmemory.MainMemoryVirtualDB;
import queryexecutor.utility.QueryExecutorUtility;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.EmptyDB;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.TableAlias;
import queryexecutor.model.database.Tuple;

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
        if (!QueryExecutorConstants.DBMS_DEBUG &&
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
            QueryExecutorUtility.addIfNotContained(result, attributeRef);
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

