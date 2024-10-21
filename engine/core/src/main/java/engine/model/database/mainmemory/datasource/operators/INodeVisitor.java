package engine.model.database.mainmemory.datasource.operators;

import engine.model.database.mainmemory.datasource.nodes.AttributeNode;
import engine.model.database.mainmemory.datasource.nodes.LeafNode;
import engine.model.database.mainmemory.datasource.nodes.MetadataNode;
import engine.model.database.mainmemory.datasource.nodes.SequenceNode;
import engine.model.database.mainmemory.datasource.nodes.SetNode;
import engine.model.database.mainmemory.datasource.nodes.TupleNode;

public interface INodeVisitor {
        
    void visitSetNode(SetNode node);
    
    void visitTupleNode(TupleNode node);
    
    void visitSequenceNode(SequenceNode node);
    
    void visitAttributeNode(AttributeNode node);
    
    void visitMetadataNode(MetadataNode node);
        
    void visitLeafNode(LeafNode node);

    Object getResult();
    
}
