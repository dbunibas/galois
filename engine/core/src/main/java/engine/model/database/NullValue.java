package engine.model.database;

import engine.EngineConstants;

public class NullValue implements IValue {

    private Object value;

    public NullValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return EngineConstants.NULL;
    }

    public Object getPrimitiveValue() {
        return this.value;
    }

    public boolean isLabeledNull() {
        return !value.toString().equals(EngineConstants.NULL_VALUE);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final NullValue other = (NullValue) obj;
        return this.value.toString().equals(other.value.toString());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
