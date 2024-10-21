package engine.model.algebra.operators.mainmemory;

import engine.model.algebra.operators.IUpdateCell;
import engine.model.database.CellRef;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.mainmemory.MainMemoryDB;
import engine.model.database.mainmemory.datasource.INode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMemoryUpdateCell implements IUpdateCell {

    private static Logger logger = LoggerFactory.getLogger(MainMemoryUpdateCell.class);

    public void execute(CellRef cellRef, IValue value, IDatabase database) {
        if (logger.isDebugEnabled()) logger.debug("Changing cell " + cellRef + " with new value " + value);
        if (logger.isTraceEnabled()) logger.trace("In database " + database);
        INode instanceRoot = ((MainMemoryDB)database).getDataSource().getInstances().get(0);
        for (INode set : instanceRoot.getChildren()) {
            if (!set.getLabel().equals(cellRef.getAttributeRef().getTableName())) {
                continue;
            }
            for (INode tuple : set.getChildren()) {
                if (!tuple.getValue().toString().equals(cellRef.getTupleOID().getValue().toString())) {
                    continue;
                }
                for (INode attribute : tuple.getChildren()) {
                    if (!attribute.getLabel().equals(cellRef.getAttributeRef().getName())) {
                        continue;
                    }
                    attribute.getChild(0).setValue(value);
                }                
            }
        }

    }
}
