package engine.persistence.xml.operators;

import java.util.List;
import engine.persistence.xml.model.AttributeDeclaration;
import engine.persistence.xml.model.ElementDeclaration;
import engine.persistence.xml.model.IXSDNode;
import engine.persistence.xml.model.PCDATA;
import engine.persistence.xml.model.SimpleType;
import engine.persistence.xml.model.TypeCompositor;

public class XSDNodeToString {
    
    public String toString(IXSDNode node) {
        XSDNodeToStringVisitor printVisitor = new XSDNodeToStringVisitor();
        node.accept(printVisitor);
        return (String)printVisitor.getResult();
    }
}

class XSDNodeToStringVisitor implements IXSDNodeVisitor {
    
    private int indentLevel = 0;
    private String treeDescription = "";
    
    public String getResult() {
        return treeDescription;
    }
    
    public void visitSimpleType(SimpleType node) {
        visitGenericNode(node);
    }
    
    public void visitElementDeclaration(ElementDeclaration node) {
        visitGenericNode(node);
    }
    
    public void visitTypeCompositor(TypeCompositor node) {
        visitGenericNode(node);
    }
    
    public void visitAttributeDeclaration(AttributeDeclaration node) {
        visitGenericNode(node);
    }
    
    public void visitPCDATA(PCDATA node) {
        visitGenericNode(node);
    }

    private void visitGenericNode(IXSDNode node) {
        treeDescription += this.indentString();
        treeDescription += node.getDescription();
        treeDescription += "\n";
        List<IXSDNode> listOfChildren = node.getChildren();
        if (listOfChildren != null) {
            this.indentLevel++;
            for (IXSDNode child : listOfChildren) {
                child.accept(this);
            }
            this.indentLevel--;
        }
    }
        
    private String indentString() {
        String result = "";
        for (int i = 0; i < this.indentLevel; i++) {
            result += "    ";
        }
        return result;
    }
    
}

