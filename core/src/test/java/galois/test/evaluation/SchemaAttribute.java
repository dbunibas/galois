package galois.test.evaluation;

import lombok.Data;
import speedy.persistence.Types;

@Data
public class SchemaAttribute {
    private String name;
    private String type;
    private Boolean nullable;
    private Boolean key = Boolean.FALSE;

    public String getSpeedyAttributeType() {
        return switch (type.toLowerCase()) {
            case Types.BOOLEAN -> Types.BOOLEAN;
            case Types.DATE -> Types.DATE;
            case Types.DATETIME -> Types.DATETIME;
            case Types.DOUBLE_PRECISION -> Types.DOUBLE_PRECISION;
            case Types.INTEGER -> Types.INTEGER;
            case Types.LONG -> Types.LONG;
            case Types.REAL -> Types.REAL;
            default -> Types.STRING;
        };
    }
}
