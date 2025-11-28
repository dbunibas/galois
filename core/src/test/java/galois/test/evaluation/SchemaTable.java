package galois.test.evaluation;

import lombok.Data;
import speedy.model.database.Attribute;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SchemaTable {
    private String name;
    private List<SchemaAttribute> attributes;

    public List<Attribute> getSpeedyAttributes() {
        return attributes.stream()
                .map(a -> new Attribute(name, a.getName(), a.getSpeedyAttributeType(), a.getNullable()))
                .toList();
    }

    public Set<String> getKeyAttributes() {
        return attributes.stream()
                .filter(SchemaAttribute::getKey)
                .map(SchemaAttribute::getName)
                .collect(Collectors.toSet());
    }
}
