package galois.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static galois.utils.FunctionalUtils.orElseThrow;

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

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true) //TODO: fix it with the corresponding not deprecated field 
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static String asString(Object value) {
        return orElseThrow(
                () -> MAPPER.writeValueAsString(value),
                MapperException::new
        );
    }

    public static Map<String, Object> fromJsonToMap(String value) {
        return orElseThrow(
                () -> value != null ?
                        MAPPER.readValue(toCleanJsonObject(value), JSON_REF) :
                        null,
                MapperException::new
        );
    }

    public static List<Map<String, Object>> fromJsonToListOfMaps(String value, boolean removeDuplicates) {
        return orElseThrow(
                () -> value != null ?
                        MAPPER.readValue(toCleanJsonList(value, removeDuplicates), LIST_OF_JSON_REF) :
                        null,
                MapperException::new
        );
    }

    public static List<String> fromJsonListToListAndRemoveDuplicates(String value) {
        return orElseThrow(
                () -> value != null ?
                        MAPPER.readValue(toCleanJsonList(value, true), LIST_STRING_REF) :
                        null,
                MapperException::new
        );
    }

    public static List<String> fromJsonListToList(String value) {
        return orElseThrow(
                () -> value != null ?
                        MAPPER.readValue(toCleanJsonList(value, false), LIST_STRING_REF) :
                        null,
                MapperException::new
        );
    }
    
    public static boolean isJSON(String response) {
        String responseList = "";
        if (response.contains("[")) responseList = toCleanJsonList(response, false);
        String responseJson = toCleanJsonObject(response);
        if (isBetween(responseList, "[", "]")) return true;
        if (isBetween(responseJson, "{", "}")) return true;
        return false;
    }

    public static String toCleanJsonObject(String response) {
        response = cleaningReasoningResponse(response);
        return getContentBetween(response, "{", "}");
    }

    private static String cleaningReasoningResponse(String response) {
        if (response.contains("<think>") && response.contains("</think>")) {
            String [] splits = response.split("</think>");
            response = splits[splits.length - 1].trim();
        }
        return response;
    }

    public static String toCleanJsonList(String response, boolean removeDuplicates) {
        response = cleaningReasoningResponse(response);
        response = cleaningJsonProlog(response);
        response = removeEmptyJsonObjects(response);
        if(!response.contains("[") && !response.contains("]") && isBetween(response, "{", "}")){ //Single object
            response = "[" + response + "]";
        }
        String cleanContent = getContentBetween(response, "[", "]");
        if (isBetween(cleanContent, "[", "]")) {
            if(removeDuplicates) {
                cleanContent = removeDuplicates(cleanContent);
            }
            return cleanContent;
        }
        String substring = "";
        if (cleanContent.contains("}") && cleanContent.contains("[")) {
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
            substring = substring.length() >= 3 ? substring.substring(0, substring.length() - 2) : "";
            substring = "[" + substring + "\n";
//            substring = cleanContent.substring(cleanContent.indexOf("["), cleanContent.lastIndexOf(",") + 1);
        }
        String jsonList = substring + "]";
        log.debug("Repaired json list: {}", jsonList);
        if(removeDuplicates) {
            jsonList = removeDuplicates(jsonList);
        }
        return jsonList;
    }

    private static String removeEmptyJsonObjects(String response) {
        return response.replaceAll("\\{}", "");
    }

    private static String cleaningJsonProlog(String response) {
        response = response.replaceAll("```json", "");
        response = response.replaceAll("```", "");
        return response.trim();
    }

    private static String removeDuplicates(String jsonList) {
        if(jsonList == null || jsonList.isBlank()){
            return jsonList;
        }
        if(!isBetween(jsonList, "[", "]")){
            return jsonList;
        }
        if(jsonList.contains("{")){
            return removeDuplicatesFromArrayOfObjects(jsonList);
        }else{
            return removeDuplicatesFromArrayOfStrings(jsonList);
        }
    }
    private static String removeDuplicatesFromArrayOfStrings(String jsonList) {
        try{
            List<String> listWithDuplicates = MAPPER.readValue(jsonList, new TypeReference<>() {});
            Set<String> addedObject = new HashSet<>();
            List<String> listWithoutDuplicates = new ArrayList<>();
            for (String obj : listWithDuplicates) {
                if(addedObject.contains(obj)) continue;
                addedObject.add(obj);
                listWithoutDuplicates.add(obj);
            }
            return MAPPER.writeValueAsString(listWithoutDuplicates);
        }catch (Exception e){
            log.warn("Unable to remove duplicates from json list: {}", jsonList, e);
            return jsonList;
        }
    }
    private static String removeDuplicatesFromArrayOfObjects(String jsonList) {
        try{
            List<Map<String, Object>> listWithDuplicates = MAPPER.readValue(jsonList, new TypeReference<>() {});
            Set<String> addedObject = new HashSet<>();
            List<Map<String, Object>> listWithoutDuplicates = new ArrayList<>();
            for (Map<String, Object> obj : listWithDuplicates) {
                String objSign = obj.toString();
                if(addedObject.contains(objSign)) continue;
                addedObject.add(objSign);
                listWithoutDuplicates.add(obj);
            }
            return MAPPER.writeValueAsString(listWithoutDuplicates);
        }catch (Exception e){
            log.debug("Unable to remove duplicates from json list: {}", jsonList);
            return jsonList;
        }
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