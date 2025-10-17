package bsf.llm.database;

import bsf.llm.models.togetherai.CellProb;
import lombok.Getter;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.TupleOID;

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
