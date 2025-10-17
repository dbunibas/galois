package queryexecutor.persistence.xml.operators;

import queryexecutor.persistence.xml.model.AttributeDeclaration;
import queryexecutor.persistence.xml.model.ElementDeclaration;
import queryexecutor.persistence.xml.model.PCDATA;
import queryexecutor.persistence.xml.model.SimpleType;
import queryexecutor.persistence.xml.model.TypeCompositor;

public interface IXSDNodeVisitor {
        
    void visitSimpleType(SimpleType node);
    
    void visitElementDeclaration(ElementDeclaration node);
    
    void visitTypeCompositor(TypeCompositor node);
    
    void visitAttributeDeclaration(AttributeDeclaration node);
    
    void visitPCDATA(PCDATA node);
    
    Object getResult();
    
}
