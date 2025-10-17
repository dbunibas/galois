package queryexecutor.model.algebra.operators.mainmemory;

import queryexecutor.model.algebra.operators.IInsertTuple;
import queryexecutor.utility.QueryExecutorUtility;
import queryexecutor.model.database.mainmemory.MainMemoryTable;
import queryexecutor.model.database.mainmemory.datasource.DataSource;
import queryexecutor.model.database.mainmemory.datasource.INode;
import queryexecutor.model.database.mainmemory.datasource.IntegerOIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.mainmemory.MainMemoryVirtualTable;

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
        INode tupleNode = QueryExecutorUtility.createNode("TupleNode", tupleLabel, tuple.getOid());
        dataSource.getInstances().get(0).addChild(tupleNode);
        for (Cell cell : tuple.getCells()) {
            if (cell.isOID()) {
                continue;
            }
            INode attributeNode = QueryExecutorUtility.createNode("AttributeNode", cell.getAttribute(), IntegerOIDGenerator.getNextOID());
            tupleNode.addChild(attributeNode);
            INode schemaNode = dataSource.getSchema().getChild(0);
            INode attributeSchemaNode = schemaNode.getChild(cell.getAttribute());
            if (attributeSchemaNode == null) {
                throw new IllegalArgumentException("Unable to find attribute " + cell.getAttribute() + " in schema " + schemaNode);
            }
            String leafLabel = attributeSchemaNode.getChild(0).getLabel();
            INode leafNode = QueryExecutorUtility.createNode("LeafNode", leafLabel, cell.getValue());
            attributeNode.addChild(leafNode);
        }
    }
}
