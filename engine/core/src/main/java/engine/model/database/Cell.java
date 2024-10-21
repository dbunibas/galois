package engine.model.database;

import engine.EngineConstants;
import java.io.Serializable;

public class Cell implements Serializable, Cloneable {

    private TupleOID tupleOid;
    private AttributeRef attributeRef;
    protected IValue value;

    public Cell(TupleOID tupleOid, AttributeRef attributeRef, IValue value) {
        this.tupleOid = tupleOid;
        this.attributeRef = attributeRef;
        this.value = value;
    }

    public Cell(CellRef cellRef, IValue value) {
        this(cellRef.getTupleOID(), cellRef.getAttributeRef(), value);
    }

    public Cell(Cell originalCell, Tuple newTuple) {
        this.tupleOid = newTuple.getOid();
        this.attributeRef = originalCell.attributeRef;
        this.value = originalCell.value;
    }

    public Cell(Cell originalCell, IValue newValue) {
        this.tupleOid = originalCell.tupleOid;
        this.attributeRef = originalCell.attributeRef;
        this.value = newValue;
    }

    public boolean isOID() {
        return attributeRef.getName().equals(EngineConstants.OID);
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public void setAttributeRef(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
    }

    public String getAttribute() {
        return attributeRef.getName();
    }

    public IValue getValue() {
        return value;
    }

    public void setValue(IValue value) {
        this.value = value;
    }

    public TupleOID getTupleOID() {
        return tupleOid;
    }

    public void setTupleOid(TupleOID tupleOid) {
        this.tupleOid = tupleOid;
    }

    public boolean isSource() {
        return this.attributeRef.isSource();
    }

    public boolean isTarget() {
        return !isSource();
    }

    public boolean isAuthoritative() {
        return this.attributeRef.isAuthoritative();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Cell other = (Cell) obj;
        return this.toHashString().equals(other.toHashString());
    }

    public boolean equalsModuloAlias(Cell other) {
        return this.getTupleOID().equals(other.getTupleOID()) && this.getAttributeRef().toStringNoAlias().equals(other.getAttributeRef().toStringNoAlias());
    }

    @Override
    public int hashCode() {
        return this.toHashString().hashCode();
    }

    @Override
    public Cell clone() {
        try {
            return (Cell) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return tupleOid + ":" + attributeRef + "-" + value + (isAuthoritative() ? " (Auth)" : "");
    }

    public String toHashString() {
        return tupleOid + ":" + attributeRef;
    }

    public String toShortString() {
        return attributeRef.getName() + ":" + value;
    }

    public String toStringWithAlias() {
        return attributeRef + ":" + value;
    }

    public String toStringWithOIDAndAlias() {
        return tupleOid + ":" + attributeRef + ":" + value;
    }
}
