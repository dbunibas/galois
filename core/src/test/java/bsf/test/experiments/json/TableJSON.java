package bsf.test.experiments.json;

import lombok.Data;

import java.util.List;

@Data
public class TableJSON {
    private String tableName;
    private List<AttributeJSON> attributes;
}
