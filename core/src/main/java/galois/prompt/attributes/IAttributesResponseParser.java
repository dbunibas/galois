package galois.prompt.attributes;

import speedy.model.database.Attribute;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface IAttributesResponseParser {
    Map<String, Object> parse(String response, List<Attribute> attributes);
}
