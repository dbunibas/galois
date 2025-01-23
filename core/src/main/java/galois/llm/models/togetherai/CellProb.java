package galois.llm.models.togetherai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CellProb {

    private String attributeName;
    private Object value;
    private Double attributeProb;
    private Double valueProb;

    public CellProb() {
    }
    
    public CellProb(String attributeName, Object value, Double attributeProb, Double valueProb) {
        this.attributeName = attributeName;
        this.value = value;
        this.attributeProb = attributeProb;
        this.valueProb = valueProb;
    }
    
//    public void setValueProb(Double valueProb) {
//        this.valueProb = valueProb;
//    }

    public String toString() {
        return attributeName + "(" + attributeProb + "): " + value + "(" + valueProb + ")";
    }

}
