package engine.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.mainmemory.datasource.DataSource;
import engine.model.database.mainmemory.datasource.INode;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import engine.model.database.mainmemory.datasource.OID;
import engine.model.database.mainmemory.datasource.nodes.AttributeNode;
import engine.model.database.mainmemory.datasource.nodes.MetadataNode;
import engine.model.database.mainmemory.datasource.nodes.SequenceNode;
import engine.model.database.mainmemory.datasource.nodes.SetNode;
import engine.model.database.mainmemory.datasource.nodes.TupleNode;

public class PersistenceUtility {

    private static Logger logger = LoggerFactory.getLogger(PersistenceUtility.class);

    public static void createEmptyTables(DataSource dataSource) {
        INode emptyInstance = PersistenceUtility.generateInstanceNode(dataSource.getSchema());
        for (INode setNode : dataSource.getSchema().getChildren()) {
            emptyInstance.addChild(PersistenceUtility.generateInstanceNode(setNode));
        }
        dataSource.addInstanceWithCheck(emptyInstance);
    }

    public static INode generateInstanceNode(INode schemaNode) {
        INode instanceNode = null;
        if (schemaNode instanceof SetNode) {
            instanceNode = new SetNode(schemaNode.getLabel(), getOID());
        } else if (schemaNode instanceof SequenceNode) {
            instanceNode = new SequenceNode(schemaNode.getLabel(), getOID());
        } else if (schemaNode instanceof TupleNode) {
            instanceNode = new TupleNode(schemaNode.getLabel(), getOID());
        } else if (schemaNode instanceof MetadataNode) {
            instanceNode = new MetadataNode(schemaNode.getLabel(), getOID());
        } else if (schemaNode instanceof AttributeNode) {
            instanceNode = new AttributeNode(schemaNode.getLabel(), getOID());
        }
        instanceNode.setRoot(schemaNode.isRoot());
        instanceNode.setVirtual(schemaNode.isVirtual());
        if (logger.isDebugEnabled()) logger.debug("Generated instance node: " + instanceNode.getLabel());
        return instanceNode;
    }

    public static OID getOID() {
        return IntegerOIDGenerator.getNextOID();
    }
}
