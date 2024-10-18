package floq.prompt.parser.attributes;

import engine.model.database.Attribute;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommaAttributesParser {
    public static Map<String, Object> parse(String response, List<Attribute> attributes) {
        List<String> cells = Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < attributes.size(); i++) {
            if (i >= cells.size()) break;
            map.put(attributes.get(i).getName(), cells.get(i));
        }
        
        return map;
    }
}
