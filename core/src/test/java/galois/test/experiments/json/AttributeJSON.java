package galois.test.experiments.json;

import lombok.Data;

@Data
public class AttributeJSON {
    private String name;
    // TODO: Handle type via enum?
    private String type;
    private Boolean nullable;
    private Boolean key = Boolean.FALSE;
}
