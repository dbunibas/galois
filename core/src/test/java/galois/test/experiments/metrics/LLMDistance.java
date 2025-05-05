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
import java.util.concurrent.TimeUnit;
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

    public boolean areCellSimilar(String expected, String actual, String commonSubstring) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        if (commonSubstring != null && getNumber(expected.replace(commonSubstring, "")) != null && getNumber(actual.replace(commonSubstring, "")) != null) {
            Number nActual = getNumber(actual.replace(commonSubstring, ""));
            Number nExpected = getNumber(expected.replace(commonSubstring, ""));
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
                    TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI);
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
                    TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI);
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

    private Number getNumber(String value) {
        try {
            Number number = NumberUtils.createNumber(value);
            return number;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

}
