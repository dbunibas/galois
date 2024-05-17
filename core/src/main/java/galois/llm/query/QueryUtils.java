package galois.llm.query;

import speedy.SpeedyConstants;
import speedy.model.database.*;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static galois.utils.Mapper.asString;

public class QueryUtils {
    public static String getKeyAsString(Key key) {
        // TODO: Check composite key
        return key.getAttributes().stream()
                .map(AttributeRef::getName)
                .collect(Collectors.joining(" and "));
    }

    public static String getAttributesAsString(Collection<Attribute> attributes) {
        return attributes.stream().map(Attribute::getName).collect(Collectors.joining(" and "));
    }

    public static Tuple createNewTupleWithMockOID(TableAlias tableAlias) {
        TupleOID mockOID = new TupleOID(IntegerOIDGenerator.getNextOID());
        Tuple tuple = new Tuple(mockOID);
        Cell oidCell = new Cell(
                mockOID,
                new AttributeRef(tableAlias, SpeedyConstants.OID),
                new ConstantValue(mockOID)
        );
        tuple.addCell(oidCell);
        return tuple;
    }

    public static String generateRegexForKeys() {
        return "[a-zA-Z, ]+";
//        return "[a-zA-Z0-9, ]+";
//        return "[a-zA-Z0-9,. ]+";
    }

    public static String generateJsonSchemaFromAttributes(ITable table, List<Attribute> attributes) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", table.getName());
        map.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        for (Attribute attribute : attributes) {
            Map<String, Object> description = new HashMap<>();
            description.put("title", attribute.getName());
            description.put("type", getJsonSchemaTypeFromDBType(attribute.getType()));
            properties.put(attribute.getName(), description);
        }
        map.put("properties", properties);

        return asString(map);
    }

    public static String generateJsonSchemaFromAttribute(ITable table, Attribute attribute) {
        return generateJsonSchemaFromAttributes(table, List.of(attribute));
    }

    private static String getJsonSchemaTypeFromDBType(String type) {
        return switch (type) {
            case "double precision", "real" -> "number";
            case "integer", "long" -> "integer";
            default -> "string";
        };
    }
}
