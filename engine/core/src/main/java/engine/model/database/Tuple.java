package engine.model.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;

import java.util.ArrayList;
import java.util.List;

public class Tuple implements Cloneable {

    private Logger logger = LoggerFactory.getLogger(Tuple.class);

    private TupleOID oid;
    private List<Cell> cells = new ArrayList<Cell>();

    public Tuple(TupleOID oid) {
        this.oid = oid;
    }

    public TupleOID getOid() {
        return oid;
    }

    public void setOid(TupleOID oid) {
        this.oid = oid;
    }
    
    public void setOidNested(TupleOID oid) {
        this.oid = oid;
        for (Cell cell : cells) {
            if (cell.isOID()) {
                cell.setValue(new ConstantValue(oid.getNumericalValue()));
                break;
            }
        }
    }

    public void addCell(Cell cell) {
        for (Cell existingCell : cells) {
            if (cell.isOID()) continue;
            if (cell.getAttributeRef().equals(existingCell.getAttributeRef())) {
                logger.warn("Unable to add cell {} in tuple {}", cell, this);
                throw new IllegalArgumentException("Tuple " + this.toStringWithOIDAndAlias() + " already contains a cell for attribute " + cell.getAttributeRef());
            }
        }
        this.cells.add(cell);
    }

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public Cell getCell(AttributeRef attribute) {
        for (Cell cell : cells) {
            if (cell.getAttributeRef().toString().equalsIgnoreCase(attribute.toString())) {
                return cell;
            }
        }
        throw new IllegalArgumentException("Unable to find cell for attribute " + attribute + " in " + this.toStringWithAlias());
    }

    public boolean containsAlias(TableAlias alias) {
        for (Cell cell : cells) {
            if (cell.getAttributeRef().getTableAlias().equals(alias)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return this.cells.size();
    }

    @Override
    public Tuple clone() {
        Tuple clone = null;
        try {
            clone = (Tuple) super.clone();
            clone.oid = new TupleOID(this.oid.getNumericalValue());
            clone.cells = new ArrayList<Cell>();
            for (Cell cell : this.cells) {
                clone.cells.add(new Cell(cell, clone));
            }
        } catch (CloneNotSupportedException ex) {
        }
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (Cell cell : cells) {
            result.append(cell.toShortString()).append(", ");
        }
        if (result.length() > 1) {
            result.deleteCharAt(result.length() - 1);
            result.deleteCharAt(result.length() - 1);
        }
        result.append("]");
        return result.toString();
    }

    public String toStringWithOID() {
//        return String.format("oid: %s %s", oid, toString());
        StringBuilder result = new StringBuilder();
        result.append("oid: ").append(oid).append(" ");
        result.append(toString());
        return result.toString();
    }

    public String toStringWithAlias() {
        StringBuilder result = new StringBuilder();
        result.append("[oid: ").append(oid).append(", ");
        for (Cell cell : cells) {
            result.append(cell.toStringWithAlias()).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
        result.append("]");
        return result.toString();
    }

    public String toStringWithOIDAndAlias() {
        StringBuilder result = new StringBuilder();
        result.append("[oid: ").append(oid).append(" ");
        for (Cell cell : cells) {
            result.append(cell.toStringWithOIDAndAlias()).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
        result.append("]");
        return result.toString();
    }

    public String toStringNoOID() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (Cell cell : cells) {
            if (cell.getAttribute().equals(EngineConstants.OID)) {
                continue;
            }
            result.append(cell.toShortString()).append(", ");
        }
        if (result.length() > 1) {
            result.deleteCharAt(result.length() - 1);
            result.deleteCharAt(result.length() - 1);
        }
        result.append("]");
        return result.toString();
    }
}
