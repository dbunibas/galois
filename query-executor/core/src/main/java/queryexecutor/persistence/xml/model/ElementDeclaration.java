package queryexecutor.persistence.xml.model;

import queryexecutor.persistence.xml.operators.IXSDNodeVisitor;

public class ElementDeclaration extends Particle {
    
    public ElementDeclaration(String label) {
        super(label);
    }

    public void accept(IXSDNodeVisitor visitor) {
        visitor.visitElementDeclaration(this);
    }

}
