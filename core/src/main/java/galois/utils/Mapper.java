package galois.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static galois.utils.FunctionalUtils.orElseThrow;

public class Mapper {
    private static final TypeReference<HashMap<String, Object>> JSON_REF = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_JSON_REF = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_STRING_REF = new TypeReference<>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static String asString(Object value) {
        return orElseThrow(
                () -> mapper.writeValueAsString(value),
                MapperException::new
        );
    }

    public static Map<String, Object> fromJsonToMap(String value) {
        return orElseThrow(
                () -> value != null ?
                        mapper.readValue(toCleanJsonObject(value), JSON_REF) :
                        null,
                MapperException::new
        );
    }

    public static List<Map<String, Object>> fromJsonToListOfMaps(String value) {
        return orElseThrow(
                () -> value != null ?
                        mapper.readValue(toCleanJsonList(value), LIST_OF_JSON_REF) :
                        null,
                MapperException::new
        );
    }

    public static List<String> fromJsonListToList(String value) {
        return orElseThrow(
                () -> value != null ?
                        mapper.readValue(toCleanJsonList(value), LIST_STRING_REF) :
                        null,
                MapperException::new
        );
    }

    private static String toCleanJsonObject(String response) {
        return getContentBetween(response, "{", "}");
    }

    private static String toCleanJsonList(String response) {
        return getContentBetween(response, "[", "]");
    }

    private static String getContentBetween(String response, String firstValue, String secondValue) {
        if (response == null || !response.contains(firstValue) || !response.contains(secondValue)) return response;
        return response.substring(response.indexOf(firstValue), response.lastIndexOf(secondValue) + 1);
    }

    private static final class MapperException extends RuntimeException {
        public MapperException(Throwable cause) {
            super(cause);
        }
    }
}
