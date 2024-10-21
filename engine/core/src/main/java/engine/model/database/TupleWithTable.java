package engine.model.database;

public class TupleWithTable implements Cloneable {

    private String table;
    private Tuple tuple;
    private boolean isForGeneration = false;

    public TupleWithTable(String table, Tuple tuple) {
        this.table = table;
        this.tuple = tuple;
    }

    public String getTable() {
        return table;
    }

    public Tuple getTuple() {
        return tuple;
    }

    public void setIsForGeneration(boolean isForGeneration) {
        this.isForGeneration = isForGeneration;
    }

    public boolean isIsForGeneration() {
        return isForGeneration;
    }

    private String hashString() {
        if (isForGeneration) {
            return new StringBuilder().append(table).append(".").append(tuple.toStringWithOID()).toString();
//            return String.format("%s.%s", table, tuple.toStringWithOID());
//            return table + "." + tuple.toStringWithOID();
        }
        return new StringBuilder().append(table).append(".").append(tuple.getOid()).toString();
//        return String.format("%s.%s", table, tuple.getOid());
//        return table + "." + tuple.getOid();
    }

    @Override
    public TupleWithTable clone() {
        TupleWithTable clone = null;
        try {
            clone = (TupleWithTable) super.clone();
            clone.table = table;
            clone.tuple = tuple.clone();
            clone.isForGeneration = isForGeneration;
        } catch (CloneNotSupportedException ex) {
        }
        return clone;
    }

    @Override
    public int hashCode() {
        return hashString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TupleWithTable other = (TupleWithTable) obj;
        return other.hashString().equals(this.hashString());
    }

    @Override
    public String toString() {
        return new StringBuilder().append(table).append(".").append(tuple.toStringWithOID()).toString();
//        return table + "." + tuple.toStringWithOID();
    }

}
