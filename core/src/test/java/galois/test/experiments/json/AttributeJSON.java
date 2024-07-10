package galois.test.experiments.json;

import lombok.Data;
import speedy.persistence.Types;

@Data
public class AttributeJSON {

    private String name;
    // TODO: Handle type via enum?
    private String type;
    private Boolean nullable;
    private Boolean key = Boolean.FALSE;

    public String getSpeedyAttributeType() {
        if (this.type.equalsIgnoreCase(Types.BOOLEAN)) {
            return Types.BOOLEAN;
        }
        if (this.type.equalsIgnoreCase(Types.DATE)) {
            return Types.DATE;
        }
        if (this.type.equalsIgnoreCase(Types.DATETIME)) {
            return Types.DATETIME;
        }
        if (this.type.equalsIgnoreCase(Types.DOUBLE_PRECISION)) {
            return Types.DOUBLE_PRECISION;
        }
        if (this.type.equalsIgnoreCase(Types.INTEGER)) {
            return Types.INTEGER;
        }
        if (this.type.equalsIgnoreCase(Types.LONG)) {
            return Types.LONG;
        }
        if (this.type.equalsIgnoreCase(Types.REAL)) {
            return Types.REAL;
        }
        return Types.STRING;
    }
}
