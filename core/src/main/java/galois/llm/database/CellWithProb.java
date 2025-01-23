package galois.llm.database;

import galois.llm.models.togetherai.CellProb;
import lombok.Getter;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.IValue;
import speedy.model.database.TupleOID;

@Getter
public class CellWithProb extends Cell {

    private CellProb cellProb;

    public CellWithProb(TupleOID tupleOid, AttributeRef attributeRef, IValue value, CellProb cellProb) {
        super(tupleOid, attributeRef, value);
        this.cellProb = cellProb;
    }

    public Double getValueProb() {
        return this.cellProb.getValueProb();
    }

    @Override
    public String toString() {
        return super.toString() + " cellProb=" + cellProb;
    }
    
    @Override
    public String toShortString() {
        return super.toShortString() + "(" + cellProb + ")";
    }

}
