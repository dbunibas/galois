package galois.test.experiments.json;

import lombok.Data;

import java.util.List;

@Data
public class SchemaJSON {
    private String name;
    private String schema;
    private String username;
    private String password;
    private List<TableJSON> tables;
}
