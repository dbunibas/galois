package galois.llm.query.utils;

import lombok.extern.slf4j.Slf4j;
import speedy.SpeedyConstants;
import speedy.model.database.*;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;
import speedy.model.expressions.Expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static galois.utils.Mapper.asString;

@Slf4j
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

    public static String getExpressionAsString(Expression expression) {
        return expression != null ? expression.getExpressionString() : null;
    }

    public static List<Attribute> getCleanAttributes(ITable table) {
        return table.getAttributes().stream().filter(a -> !a.getName().equalsIgnoreCase("oid")).toList();
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

    public static Tuple mapToTuple(Map<String, Object> map, TableAlias tableAlias, List<Attribute> attributes) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        return mapToTuple(tuple, map, tableAlias, attributes);
    }

    public static Tuple mapToTuple(Tuple tuple, Map<String, Object> map, TableAlias tableAlias, List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            IValue cellValue = map.containsKey(attribute.getName()) &&
                    map.get(attribute.getName()) != null && 
                    !map.get(attribute.getName()).equals("null") ?
                    new ConstantValue(map.get(attribute.getName())) :
                    new NullValue(SpeedyConstants.NULL_VALUE);
            Cell currentCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attribute.getName()),
                    cellValue
            );
            tuple.addCell(currentCell);
        }
        if (tuple.getCells().size() <= 1) return null;
        return tuple;
    }

    public static Tuple mapToTupleIgnoreMissingAttributes(Map<String, Object> map, TableAlias tableAlias) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        for (String attributeName : map.keySet()) {
            IValue cellValue = !map.get(attributeName).equals("null")
                    ? new ConstantValue(map.get(attributeName))
                    : new NullValue(SpeedyConstants.NULL_VALUE);
            Cell currentCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attributeName),
                    cellValue
            );
            tuple.addCell(currentCell);

        }
        return tuple;
    }

    public static String generateRegexForKeys() {
        return "[a-zA-Z, ]+";
//        return "[a-zA-Z0-9, ]+";
//        return "[a-zA-Z0-9,. ]+";
    }

    public static String generateJsonSchemaForKeys(ITable table) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", table.getName());
        map.put("type", "array");

        Map<String, Object> items = new HashMap<>();
        items.put("type", "string");
        map.put("items", items);

        return asString(map);
    }

    public static String generateJsonSchemaFromAttribute(ITable table, Attribute attribute) {
        return generateJsonSchemaFromAttributes(table, List.of(attribute));
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

    public static String generateJsonSchemaListFromAttributes(ITable table, List<Attribute> attributes) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", table.getName());
        map.put("type", "array");

        Map<String, Object> items = new HashMap<>();
        items.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        for (Attribute attribute : attributes) {
            Map<String, Object> description = new HashMap<>();
            description.put("title", attribute.getName());
            description.put("type", getJsonSchemaTypeFromDBType(attribute.getType()));
            properties.put(attribute.getName(), description);
        }
        items.put("properties", properties);

        map.put("items", items);

        return asString(map);
    }

    private static String getJsonSchemaTypeFromDBType(String type) {
        return switch (type.toLowerCase()) {
            case "double precision", "real", "float8" -> "number";
            case "integer", "long" -> "integer";
            default -> "string";
        };
    }
}
