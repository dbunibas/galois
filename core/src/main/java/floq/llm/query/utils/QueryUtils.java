package floq.llm.query.utils;

import lombok.extern.slf4j.Slf4j;
import engine.EngineConstants;
import engine.exceptions.DAOException;
import engine.model.database.*;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import engine.model.expressions.Expression;
import engine.persistence.Types;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static floq.utils.Mapper.asString;
import static engine.persistence.Types.BOOLEAN;

@Slf4j
public class QueryUtils {

    public static final String NORMALIZE_UPPER_CASE = "upperCase";
    public static final String NORMALIZE_LOWER_CASE = "lowerCase";

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
                new AttributeRef(tableAlias, EngineConstants.OID),
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
            IValue cellValue = safelyParseCellValue(map, attribute);
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

    private static IValue safelyParseCellValue(Map<String, Object> map, Attribute attribute) {
        if (map == null || map.get(attribute.getName()) == null || map.get(attribute.getName()).toString().equalsIgnoreCase("null")) {
            return new NullValue(EngineConstants.NULL_VALUE);
        }

        Object value = map.get(attribute.getName());
        if (BOOLEAN.equalsIgnoreCase(attribute.getType())) {
            if(value.toString().equalsIgnoreCase("YES")) return new ConstantValue(Boolean.TRUE);
            if(value.toString().equalsIgnoreCase("NO")) return new ConstantValue(Boolean.FALSE);
            return new ConstantValue(Boolean.parseBoolean(value.toString()));
        }
        if (!Types.isNumerical(attribute.getType())) {
            return new ConstantValue(value);
        }

        String valueAsString = value.toString();
        // Replace commas with dots
        if (valueAsString.contains(",")) {
            valueAsString = valueAsString.replaceAll(",", ".");
        }
        // Replace K (or k) with 000
        if (valueAsString.contains("K") || valueAsString.contains("k")) {
            valueAsString = valueAsString.replaceAll("K", "000");
            valueAsString = valueAsString.replaceAll("k", "000");
        }
        // Replace M (or m) with 000000
        if (valueAsString.contains("M") || valueAsString.contains("m")) {
            valueAsString = valueAsString.replaceAll("M", "000000");
            valueAsString = valueAsString.replaceAll("m", "000000");
        }

        try {
            Object typedValue = Types.getTypedValue(attribute.getType(), valueAsString);
            return new ConstantValue(typedValue);
        } catch (DAOException e) {
            return new ConstantValue(valueAsString);
        }
    }

    public static Tuple mapToTupleIgnoreMissingAttributes(Map<String, Object> map, TableAlias tableAlias) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        for (String attributeName : map.keySet()) {
            IValue cellValue = null;
            if (map.get(attributeName) == null || map.get(attributeName).toString().equalsIgnoreCase("null")) {
                cellValue = new NullValue(EngineConstants.NULL_VALUE);
            } else {
                cellValue = new ConstantValue(map.get(attributeName));
            }
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

    public static String generateJsonSchemaForPrimaryKey(ITable table) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", table.getName());
        map.put("type", "array");

        Map<String, Object> items = new HashMap<>();
        items.put("type", "string");
        map.put("items", items);

        return asString(map);
    }

    public static String generateJsonSchemaForCompositePrimaryKey(ITable table, Key primaryKey) {
        List<Attribute> attributes = primaryKey.getAttributes().stream()
                .map(a -> table.getAttribute(a.getName()))
                .toList();
        return generateJsonSchemaListFromAttributes(table, attributes);
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

    public static Tuple normalizeTextValues(Tuple tuple, String normalization) {
        Tuple cloned = tuple.clone();
        for (Cell cell : cloned.getCells()) {
            if (cell.getValue() instanceof NullValue) continue;
            if (normalization.equalsIgnoreCase(NORMALIZE_LOWER_CASE)) {
                IValue normalizedValue = new ConstantValue(cell.getValue().toString().toLowerCase());
                cell.setValue(normalizedValue);
            }
            if (normalization.equalsIgnoreCase(NORMALIZE_UPPER_CASE)) {
                IValue normalizedValue = new ConstantValue(cell.getValue().toString().toUpperCase());
                cell.setValue(normalizedValue);
            }
        }
        return cloned;
    }

    private static String getJsonSchemaTypeFromDBType(String type) {
        return switch (type.toLowerCase()) {
            case "double precision", "real", "float8" -> "number";
            case "integer", "long" -> "integer";
            default -> "string";
        };
    }

    public static boolean isAlreadyContained(Tuple tuple, List<Tuple> tuples) {
        return tuples.stream().map(Tuple::toStringNoOID).collect(Collectors.toSet()).contains(tuple.toStringNoOID());
    }
}
