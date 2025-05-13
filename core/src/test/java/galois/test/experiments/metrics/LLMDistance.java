package galois.test.experiments.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.Choice;
import galois.llm.models.togetherai.Message;
import galois.llm.models.togetherai.ResponseTogetherAI;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.utils.cache.CacheEntry;
import galois.llm.query.utils.cache.LLMCache;
import galois.utils.Mapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;

@Slf4j
public class LLMDistance {

    private String modelName = TogetherAIConstants.MODEL_LLAMA3_1_8B;
    private TogetherAIModel llmModel = new TogetherAIModel(Constants.TOGETHERAI_API, modelName, true);
    private ObjectMapper objectMapper = Mapper.MAPPER;
    private Double thresholdForNumeric = 0.1;

    private String promptCell = "Given the following two cells, identify if they represent the same real entity.\n"
            + "\n"
            + "cell1: $CELL_1$\n"
            + "cell2: $CELL_2$\n"
            + "\n"
            + "Answer only with 'yes' or 'no'.";

    private String promptTuple = "Given the following two tuples, identify if they represent the same real entity.\n"
            + "\n"
            + "tuple1: $TUPLE_1$\n"
            + "tuple2: $TUPLE_2$\n"
            + "\n"
            + "Answer only with 'yes' or 'no'.";

    private String promptCellPartitions = "Given a single value and an array of values in JSON format, identify and return the first value in the array that represents the same real-world entity as the single value. If multiple matches exist, return the first occurrence. If no match is found, return \\\"\\\". The result should be in JSON format with a 'result' property.\n"
            + "\n"
            + "Example Input:\n"
            + "Attribute: country\n"
            + "Single value: 'United States'\n"
            + "Array: ['Italy', 'United States of America', 'USA', 'Germany']\n"
            + "Example Output:\n"
            + "{\\\"result\\\": 'United States of America'}\n"
            + "\n"
            + "Instructions:\n"
            + "\n"
            + "Input: A single value and a JSON array.\n"
            + "Task: Find the first matching entity in the array.\n"
            + "Output: Return the match in JSON object format or '{}' if none found. Respond with a JSON object containing only the answer. Do not include explanations or comments.\n"
            + "\n"
            + "Input:\n"
            + "Attribute: $ATTRIBUTE$\n"
            + "Single value: '$VALUE$'\n"
            + "Array: [$ARRAY$]\n"
            + "\n"
            + "Output:\n";

    public boolean areCellSimilar(String expected, String actual, String commonSubstring) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
//        log.error("Compare: " + actual + " --- " + expected + " **** " + commonSubstring);
        if (commonSubstring != null && getNumber(expected.replace(commonSubstring, "").trim()) != null && getNumber(actual.replace(commonSubstring, "").trim()) != null) {
            Number nActual = getNumber(actual.replace(commonSubstring, ""));
            Number nExpected = getNumber(expected.replace(commonSubstring, ""));
//            log.error("Compare numerical values: " + nActual +  " ---" + nExpected);
            if (thresholdForNumeric != null) {
                return Math.abs((nExpected.floatValue() - nActual.floatValue()) / nExpected.floatValue()) <= thresholdForNumeric;
            } else {
                return nActual.floatValue() == nExpected.floatValue();
            }
        }
        LLMCache llmCache = LLMCache.getInstance();
        String editedPrompt = promptCell.replace("$CELL_1$", expected).replace("$CELL_2$", actual);
        String response = null;
        if (llmCache.containsQuery(editedPrompt, 0, null, editedPrompt)) {
            //log.debug("Cache hit for {}, returning cached value!", editedPrompt);
            CacheEntry entry = llmCache.getResponse(editedPrompt, 0, null, editedPrompt);
            response = entry.response();
        } else {
            String swappedEditedPrompt = promptCell.replace("$CELL_1$", actual).replace("$CELL_2$", expected); // check on the other side in the cache
            if (llmCache.containsQuery(swappedEditedPrompt, 0, null, swappedEditedPrompt)) {
                //log.debug("Cache hit for {}, returning cached value!", swappedEditedPrompt);
                CacheEntry entry = llmCache.getResponse(swappedEditedPrompt, 0, null, swappedEditedPrompt);
                response = entry.response();
            } else {
                try {
//                    TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI);
                    TimeUnit.MILLISECONDS.sleep((long) 100);
//                    log.error("compare using LLM: \n" + editedPrompt);
                    response = llmModel.getModelResponse(editedPrompt);
                    llmCache.updateCache(editedPrompt, 0, null, editedPrompt, response, 0, 0, 0, 0);
                } catch (Exception e) {
                    log.error("Exception in making the request: {}", e);
                }
            }
        }
        if (response == null || response.isBlank()) {
            return false;
        }
        //log.debug("Response: {}", response);
        try {
            ResponseTogetherAI responseAPI = objectMapper.readValue(response, ResponseTogetherAI.class);
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            String content = message.getContent();
            return content.equalsIgnoreCase("yes");
        } catch (JsonProcessingException exception) {

        }
        return false;
    }

    public boolean areTupleSimilar(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        LLMCache llmCache = LLMCache.getInstance();
        String editedPrompt = promptTuple.replace("$TUPLE_1$", expected).replace("$TUPLE_2$", actual);
        String response = null;
        if (llmCache.containsQuery(editedPrompt, 0, null, editedPrompt)) {
            //log.debug("Cache hit for {}, returning cached value!", editedPrompt);
            CacheEntry entry = llmCache.getResponse(editedPrompt, 0, null, editedPrompt);
            response = entry.response();
        } else {
            String swappedEditedPrompt = promptTuple.replace("$TUPLE_1$", actual).replace("$TUPLE_2$", expected); // check on the other side in the cache
            if (llmCache.containsQuery(swappedEditedPrompt, 0, null, swappedEditedPrompt)) {
                //log.debug("Cache hit for {}, returning cached value!", swappedEditedPrompt);
                CacheEntry entry = llmCache.getResponse(swappedEditedPrompt, 0, null, swappedEditedPrompt);
                response = entry.response();
            } else {
                try {
//                    TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI);
                    TimeUnit.MILLISECONDS.sleep((long) 100);
                    response = llmModel.getModelResponse(editedPrompt);
                    llmCache.updateCache(editedPrompt, 0, null, editedPrompt, response, 0, 0, 0, 0);
                } catch (Exception e) {
                    log.error("Exception in making the request: {}", e);
                }
            }
        }
        if (response == null || response.isBlank()) {
            return false;
        }
        //log.debug("Response: {}", response);
        try {
            ResponseTogetherAI responseAPI = objectMapper.readValue(response, ResponseTogetherAI.class);
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            String content = message.getContent();
            //log.debug("Content: " + content);
            return content.equalsIgnoreCase("yes");
        } catch (JsonProcessingException exception) {

        }
        return false;
    }
    
    public String findSimilar(String attribute, String value, Set<String> candidates) {
        //if (true) return null;
        if (candidates.isEmpty()) return null;
        if (getNumber(value) != null) {
            return findNumericSimilar(attribute, value, candidates);
        }
        String arrayString = candidates.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
        String prompt = promptCellPartitions.replace("$ATTRIBUTE$", attribute);
        prompt = prompt.replace("$VALUE$", value);
        prompt = prompt.replace("$ARRAY$", arrayString);
        LLMCache llmCache = LLMCache.getInstance();
//        log.error("Prompt: {}", prompt);
        String response = null;
        if (llmCache.containsQuery(prompt, 0, null, prompt)) {
            //log.debug("Cache hit for {}, returning cached value!", editedPrompt);
            CacheEntry entry = llmCache.getResponse(prompt, 0, null, prompt);
            response = entry.response();
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI);
                response = llmModel.getModelResponse(prompt);
                llmCache.updateCache(prompt, 0, null, prompt, response, 0, 0, 0, 0);
            } catch (Exception e) {
                log.error("Exception in making the request: {}", e);
            }
        }
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            ResponseTogetherAI responseAPI = objectMapper.readValue(response, ResponseTogetherAI.class);
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            String content = message.getContent();
            Map<String, Object> responseMap = Mapper.fromJsonToMap(content);
            if (responseMap == null || responseMap.isEmpty()) return null;
            return responseMap.get("result").toString();
        } catch (Exception exception) {
            return null;
        }
    }

    public Number getNumber(String value) {
        try {
//            log.error("Value: " + value);
            Number number = NumberUtils.createNumber(value);
//            log.error(number.toString());
            return number;
        } catch (NumberFormatException nfe) {
            try {
                value = value.replace(",", ".");
                Number number = NumberUtils.createNumber(value);
                return number;
            } catch (NumberFormatException internalNfe) {
                return null;
            }
        }
    }

    private String findNumericSimilar(String attribute, String value, Set<String> candidates) {
        Number nActual = getNumber(value);
        return null;
    }

}
