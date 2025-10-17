package queryexecutor.model.database.mainmemory.datasource.operators;

import queryexecutor.model.database.mainmemory.datasource.nodes.AttributeNode;
import queryexecutor.model.database.mainmemory.datasource.nodes.LeafNode;
import queryexecutor.model.database.mainmemory.datasource.nodes.MetadataNode;
import queryexecutor.model.database.mainmemory.datasource.nodes.SequenceNode;
import queryexecutor.model.database.mainmemory.datasource.nodes.SetNode;
import queryexecutor.model.database.mainmemory.datasource.nodes.TupleNode;

public interface INodeVisitor {
        
    void visitSetNode(SetNode node);
    
    void visitTupleNode(TupleNode node);
    
    void visitSequenceNode(SequenceNode node);
    
    void visitAttributeNode(AttributeNode node);
    
    void visitMetadataNode(MetadataNode node);
        
    void visitLeafNode(LeafNode node);

    Object getResult();
    
}
