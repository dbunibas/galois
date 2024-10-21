package engine.persistence.xml.model;

import engine.persistence.xml.operators.IXSDNodeVisitor;

public class AttributeDeclaration extends Particle {
    
    public AttributeDeclaration(String label) {
        super(label);
    }

    public void accept(IXSDNodeVisitor visitor) {
        visitor.visitAttributeDeclaration(this);
    }

}
