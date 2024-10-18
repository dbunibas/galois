package floq.prompt.parser.entities;

import engine.EngineConstants;
import engine.model.database.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MistralTableEntitiesParser {
    public static List<Map<String, Object>> parse(String response, ITable table) {
        return Arrays.stream(response.split("\n"))
                .skip(2)
                .map(row -> toMap(row, table))
                .toList();
    }

    private static Map<String, Object> toMap(String row, ITable table) {
        Map<String, Object> map = new HashMap<>();

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid"))
                .toList();
        List<String> cells = Arrays.stream(row.trim().split("\\|"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            IValue value = cells.size() > i ?
                    new ConstantValue(cells.get(i)) :
                    new NullValue(EngineConstants.NULL_VALUE);
            map.put(attribute.getName(), value);
        }

        return map;
    }
}
