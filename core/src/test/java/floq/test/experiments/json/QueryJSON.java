package floq.test.experiments.json;

import lombok.Data;

@Data
public class QueryJSON {
    private String sql;
    private SchemaJSON schema;
    private String results;
}
