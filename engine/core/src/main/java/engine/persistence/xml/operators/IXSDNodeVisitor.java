package engine.persistence.xml.operators;

import engine.persistence.xml.model.AttributeDeclaration;
import engine.persistence.xml.model.ElementDeclaration;
import engine.persistence.xml.model.PCDATA;
import engine.persistence.xml.model.SimpleType;
import engine.persistence.xml.model.TypeCompositor;

public interface IXSDNodeVisitor {
        
    void visitSimpleType(SimpleType node);
    
    void visitElementDeclaration(ElementDeclaration node);
    
    void visitTypeCompositor(TypeCompositor node);
    
    void visitAttributeDeclaration(AttributeDeclaration node);
    
    void visitPCDATA(PCDATA node);
    
    Object getResult();
    
}
