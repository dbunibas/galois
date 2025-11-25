package galois.test.evaluation;

import lombok.Data;

import java.util.List;

@Data
public class SchemaTable {
    private String name;
    private List<SchemaAttribute> attributes;
}
