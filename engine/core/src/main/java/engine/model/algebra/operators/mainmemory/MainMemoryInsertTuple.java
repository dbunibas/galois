package engine.model.algebra.operators.mainmemory;

import engine.model.algebra.operators.IInsertTuple;
import engine.utility.EngineUtility;
import engine.model.database.mainmemory.MainMemoryTable;
import engine.model.database.mainmemory.datasource.DataSource;
import engine.model.database.mainmemory.datasource.INode;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.Cell;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.Tuple;
import engine.model.database.mainmemory.MainMemoryVirtualTable;

public class MainMemoryInsertTuple implements IInsertTuple {

    private static Logger logger = LoggerFactory.getLogger(MainMemoryInsertTuple.class);

    @Override
    public void execute(ITable table, Tuple tuple, IDatabase source, IDatabase target) {
        if (logger.isDebugEnabled()) logger.debug("----Executing insert into table " + table.getName() + " tuple: " + tuple);
        if (table instanceof MainMemoryVirtualTable) {
            throw new IllegalArgumentException("Unable to insert tuple in virtual tables");
        }
        DataSource dataSource = ((MainMemoryTable) table).getDataSource();
        String tupleLabel = dataSource.getSchema().getChild(0).getLabel();
        INode tupleNode = EngineUtility.createNode("TupleNode", tupleLabel, tuple.getOid());
        dataSource.getInstances().get(0).addChild(tupleNode);
        for (Cell cell : tuple.getCells()) {
            if (cell.isOID()) {
                continue;
            }
            INode attributeNode = EngineUtility.createNode("AttributeNode", cell.getAttribute(), IntegerOIDGenerator.getNextOID());
            tupleNode.addChild(attributeNode);
            INode schemaNode = dataSource.getSchema().getChild(0);
            INode attributeSchemaNode = schemaNode.getChild(cell.getAttribute());
            if (attributeSchemaNode == null) {
                throw new IllegalArgumentException("Unable to find attribute " + cell.getAttribute() + " in schema " + schemaNode);
            }
            String leafLabel = attributeSchemaNode.getChild(0).getLabel();
            INode leafNode = EngineUtility.createNode("LeafNode", leafLabel, cell.getValue());
            attributeNode.addChild(leafNode);
        }
    }
}
