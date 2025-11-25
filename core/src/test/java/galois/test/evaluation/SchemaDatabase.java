package galois.test.evaluation;

import lombok.Data;

import java.util.List;

@Data
public class SchemaDatabase {
    private String dbName;
    private List<SchemaTable> tables;
}
