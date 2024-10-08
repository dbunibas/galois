package galois.llm.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import galois.Constants;
import galois.llm.models.togetherai.*;
import galois.llm.query.LLMQueryStatManager;
import galois.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TogetherAIModel implements IModel, ChatLanguageModel {

    private String toghetherAiAPI;
    private String modelName;
    private boolean streamMode;
    private int maxTokens = 4096; // max returned tokens
    private double temperature = 0.0; // 0.0 deterministic
    private double topP = 0.0; // 0.0 deterministic
    private List<Message> messages = new ArrayList<>();
    private ObjectMapper objectMapper = Mapper.MAPPER;
    private int inputTokens = 0;
    private int outputTokens = 0;
    private boolean checkJSON = true;
    private boolean checkJSONResponseContent = false;
    private Map<String, String> inMemoryCache = new HashMap<>(); // TODO: do we need to save it?

//    public TogetherAIModel(String toghetherAiAPI, String modelName) {
//        this(toghetherAiAPI, modelName, false);
//    }

    public TogetherAIModel(String toghetherAiAPI, String modelName, boolean streamMode) {
        this.toghetherAiAPI = toghetherAiAPI;
        this.modelName = modelName;
        this.streamMode = streamMode;
    }

    @Override
    public String text(String prompt) {
        ResponseTogetherAI responseAPI = getResponse(prompt);
        if (responseAPI != null) {
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            if (message != null) {
                return message.getContent().trim();
            }
        }
        return null;
    }

    public ResponseTogetherAI getResponse(String prompt) {
        Message newMessage = new Message();
        newMessage.setRole(TogetherAIConstants.USER);
        newMessage.setContent(prompt);
        String jsonRequest = getJsonForRequest();
        if (jsonRequest.isEmpty()) {
            log.trace("Return null because jsonRequest is empty: " + jsonRequest);
            return null;
        }
        String authorizationValue = "Bearer " + this.toghetherAiAPI;
        log.trace("Reset retry to 0");
        long start = System.currentTimeMillis();
        String response = this.inMemoryCache.get(jsonRequest);
        if (response == null && !this.inMemoryCache.containsKey(jsonRequest)) {
            response = this.makeRequest(jsonRequest, authorizationValue);
            this.inMemoryCache.put(jsonRequest, response);
        }
        if (response == null || response.isEmpty()) {
            log.trace("Return null because response is null or empty: " + response);
            return null;
        }
        try {
            ResponseTogetherAI responseAPI = objectMapper.readValue(response, ResponseTogetherAI.class);
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            if (message != null) {
                if (checkJSONResponseContent) {
                    String content = message.getContent();
                    if (content != null && !Mapper.isJSON(content)) {
                        if (!this.messages.isEmpty()) {
                            this.messages.remove(this.messages.size() - 1);
                        }
                        log.trace("Return null because content is not a JSON: \n" + content);
                        return null;
                    }
                }
                Usage usage = responseAPI.getUsage();
                LLMQueryStatManager.getInstance().updateBaseLLMRequest(1);
                LLMQueryStatManager.getInstance().updateBaseLLMTokensInput(usage.getPromptTokens());
                LLMQueryStatManager.getInstance().updateBaseLLMTokensOutput(usage.getCompletionTokens());
                LLMQueryStatManager.getInstance().updateBaseTimeMs((System.currentTimeMillis() - start));
                this.inputTokens += usage.getPromptTokens();
                this.outputTokens += usage.getCompletionTokens();
                log.trace("Add Assistant Message in getResponse: {}", newMessage);
                this.messages.add(newMessage);
                this.messages.add(message);
            }
            return responseAPI;
        } catch (Exception e) {
            log.error("Exception with parsing response: " + response, e);
        }
        log.trace("Return null because there was an exception.");
        return null;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        log.trace("Messages: " + messages);
        log.trace("Clear all messages!");
        this.messages.clear();
        String lastTextMessage = null;
        for (ChatMessage message : messages) {
            if (message.type().equals(ChatMessageType.USER)) {
                Message mex = new Message();
                mex.setRole(TogetherAIConstants.USER);
                mex.setContent(message.text());
                log.trace("Add User Message: " + mex.getContent());
                this.messages.add(mex);
            } else {
                Message mex = new Message();
                mex.setRole(TogetherAIConstants.ASSISTANT);
                mex.setContent(Mapper.toCleanJsonList(message.text(), true));
                //mex.setContent(message.text());
                log.trace("Add Assistant message: {}", mex);
                this.messages.add(mex);
            }
            lastTextMessage = message.text();
        }
        log.trace("Messages: " + this.messages.size());
        log.trace("Last Message: " + lastTextMessage);
        if (lastTextMessage != null) {
            ResponseTogetherAI responseAPI = getResponse(lastTextMessage);
            if (responseAPI != null) {
                Choice choice = responseAPI.getChoices().get(0);
                Message message = choice.getMessage();
                if (message != null) {
                    return Response.from(AiMessage.from(message.getContent()), new TokenUsage(responseAPI.getUsage().getPromptTokens(), responseAPI.getUsage().getCompletionTokens()));
                }
            }
            log.trace("Return null because responseAPI is null");
            return null;
        }
        log.trace("Return null because lastTextMessage is null");
        return null;
    }

    private String makeRequest(String jsonRequest, String authorizationValue) {
        int numRetry = 0;
        while (numRetry < TogetherAIConstants.MAX_RETRY) {
            HttpURLConnection connection = null;
            try {
                TimeUnit.MILLISECONDS.sleep((long) Constants.WAIT_TIME_MS_TOGETHERAI * (numRetry + 1));
                URL url = URI.create(TogetherAIConstants.BASE_ENDPOINT + "chat/completions").toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TogetherAIConstants.CONNECTION_TIMEOUT);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", authorizationValue);
                connection.setDoOutput(true);
                String responseText;
                if(streamMode) {
                    responseText = getStringStreamMode(jsonRequest, connection);
                }else{
                    responseText = getStringStandardMode(jsonRequest, connection);
                }
                if (checkJSON && !Mapper.isJSON(responseText)) {
                    log.trace("The response is not a JSON: \n{}. Retrying", responseText);
                    numRetry++;
                    continue;
                }
                return responseText;
            } catch (Exception e) {
                if (e instanceof IOException) {
                    log.trace("Request attempt number {} failed with exception: {}", numRetry, e.getMessage(), e);
                    numRetry++;
                } else {
                    log.error("Generic Exception with the request. Skipping retry", e);
                    return null;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        log.error("Return null because reached max retry: {}", TogetherAIConstants.MAX_RETRY);
        return null;
    }

    private @NotNull String getStringStreamMode(String jsonRequest, HttpURLConnection connection) throws IOException {
        log.debug("# API Request [{}] - {}\nBody: {}", connection.getRequestMethod(), connection.getURL(), jsonRequest);
        OutputStream os = connection.getOutputStream();
        byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
        os.flush();
        connection.connect();
        if (connection.getResponseCode() != 200) {
            log.error("Error response: {}", IOUtils.toString(new InputStreamReader(connection.getErrorStream())));
            throw new IOException("Error response " + connection.getErrorStream());
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        ResponseTogetherAI finalResponse = new ResponseTogetherAI();
        StringBuilder messageContent = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            if (!responseLine.startsWith("data: ")) continue;
            String dataChunk = responseLine.substring("data: ".length());
            if (dataChunk.equals("[DONE]")) {
                break;
            }
            StreamResponse streamResponse = objectMapper.readValue(dataChunk, StreamResponse.class);
            finalResponse.setUsage(streamResponse.getUsage());
            if(!streamResponse.choices.isEmpty()){
                messageContent.append(streamResponse.choices.get(0).delta.content);
            }
        }
        Choice choice = new Choice();
        Message message = new Message();
        message.setContent(messageContent.toString());
        choice.setMessage(message);
        finalResponse.setChoices(new ArrayList<>());
        finalResponse.getChoices().add(0, choice);
        String responseText = objectMapper.writeValueAsString(finalResponse);
        log.debug("# API Response {}: \n{}", connection.getResponseCode(), responseText);
        return responseText;
    }

    private @NotNull String getStringStandardMode(String jsonRequest, HttpURLConnection connection) throws IOException {
        log.debug("# API Request [{}] - {}\nBody: {}", connection.getRequestMethod(), connection.getURL(), jsonRequest);
        OutputStream os = connection.getOutputStream();
        byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
        os.flush();
        connection.connect();
        if (connection.getResponseCode() != 200) {
            log.error("Error response: {}", IOUtils.toString(new InputStreamReader(connection.getErrorStream())));
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        String responseText = response.toString().trim();
        log.debug("# API Response {}: \n{}", connection.getResponseCode(), responseText);
        return responseText;
    }

    private String getJsonForRequest() {
        String jsonReturn = "{\n"
                + "    \"model\": \"{$MODEL_NAME$}\",\n"
                + "    \"max_tokens\": {$MAX_TOKENS$},\n"
                + "    \"temperature\": {$TEMPERATURE$},\n"
                + "    \"top_p\": {$TOP_P$},\n"
                + "    \"top_k\": 50,\n"
                + "    \"repetition_penalty\": 1,\n"
                + "    \"stop\": [\n"
                + "        \"<|eot_id|>\"\n"
                + "    ],\n"
                + (streamMode ? "    \"stream\": true,\n" : "")
                + (streamMode ? "    \"stream_tokens\": true,\n" : "")
//                + "    \"stream\": true,\n"
                + "    \"messages\": {$MESSAGES$}\n"
                + "}";
        jsonReturn = jsonReturn.replace("{$MODEL_NAME$}", this.modelName);
        jsonReturn = jsonReturn.replace("{$MAX_TOKENS$}", this.maxTokens + "");
        jsonReturn = jsonReturn.replace("{$TEMPERATURE$}", this.temperature + "");
        jsonReturn = jsonReturn.replace("{$TOP_P$}", this.topP + "");
        try {
            String jsonMessages = objectMapper.writeValueAsString(this.messages);
//            log.trace("Json Messages: " + jsonMessages);
            jsonReturn = jsonReturn.replace("{$MESSAGES$}", jsonMessages);
//            log.trace("JsonReturn: \n " + jsonReturn);
            return jsonReturn.trim();
        } catch (JsonProcessingException jpe) {
            log.error("Error generating the json: " + jpe);
        }
        String s = "{  \"$id\": \"https://example.com/person.schema.json\",  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",  \"title\": \"Person\",  \"type\": \"object\",  \"properties\": {    \"firstName\": {      \"type\": \"string\",      \"description\": \"The person's first name.\"    },    \"lastName\": {      \"type\": \"string\",      \"description\": \"The person's last name.\"    },    \"age\": {      \"description\": \"Age in years which must be equal to or greater than zero.\",      \"type\": \"integer\",      \"minimum\": 0    }  }}";
        return null;
    }

    private boolean isValid(String json) {
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }
}
