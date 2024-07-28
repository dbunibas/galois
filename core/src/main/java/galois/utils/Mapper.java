package galois.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static galois.utils.FunctionalUtils.orElseThrow;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    
    public static boolean isJSON(String response) {
        String responseList = "";
        if (response.contains("[")) responseList = toCleanJsonList(response);
        String responseJson = toCleanJsonObject(response);
        if (isBetween(responseList, "[", "]")) return true;
        if (isBetween(responseJson, "{", "}")) return true;
        return false;
    }

    private static String toCleanJsonObject(String response) {
        return getContentBetween(response, "{", "}");
    }

    public static String toCleanJsonList(String response) {
        String cleanContent = getContentBetween(response, "[", "]");
        if (isBetween(cleanContent, "[", "]")) return cleanContent;
        String substring = "";
        if (cleanContent.contains("}")) {
            substring = cleanContent.substring(cleanContent.indexOf("["), cleanContent.lastIndexOf("}") + 1);
        } else {
            Pattern pattern = Pattern.compile("\"(.*?)\"");
            Matcher matcher = pattern.matcher(cleanContent);
            List<String> keys = new ArrayList<>();
            while(matcher.find()) {
                keys.add(matcher.group(1));
            }
            for (String key : keys) {
                substring += '"' + key + '"'+",\n";
            }
            substring = substring.substring(0, substring.length() - 2);
            substring = "[" + substring + "\n";
//            substring = cleanContent.substring(cleanContent.indexOf("["), cleanContent.lastIndexOf(",") + 1);
        }
        String jsonList = substring + "]";
        log.debug("Repaired json list: {}", jsonList);
        return jsonList;
    }

    private static String getContentBetween(String response, String firstValue, String secondValue) {
        if (response == null || !response.contains(firstValue) || !response.contains(secondValue)) return response;
        return response.substring(response.indexOf(firstValue), response.lastIndexOf(secondValue) + 1);
    }

    private static boolean isBetween(String response, String start, String end) {
        return response.startsWith(start) && response.endsWith(end);
    }

    private static final class MapperException extends RuntimeException {
        public MapperException(Throwable cause) {
            super(cause);
        }
    }
}
