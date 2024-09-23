package galois.llm.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import galois.Constants;
import galois.llm.models.togetherai.Choice;
import galois.llm.models.togetherai.Message;
import galois.llm.models.togetherai.ResponseTogetherAI;
import galois.llm.models.togetherai.Usage;
import galois.utils.Mapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TogetherAIModel implements IModel, ChatLanguageModel {

    public static final String MODEL_LLAMA3_8B = "meta-llama/Llama-3-8b-chat-hf";
    public static final String MODEL_LLAMA3_70B = "meta-llama/Llama-3-70b-chat-hf";
    public static final String MODEL_LLAMA3_1_8B = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo";
    public static final String MODEL_LLAMA3_1_70B = "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    private String endPoint = "https://api.together.xyz/v1/chat/completions";
    private String toghetherAiAPI;
    private String modelName;
    private int maxTokens = 4096; // max returned tokens
    private double temperature = 0.0; // 0.0 deterministic
    private double topP = 0.0; // 0.0 deterministic
    private List<Message> messages = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private int inputTokens = 0;
    private int outputTokens = 0;
    private int waitTime = Constants.WAIT_TIME_MS_TOGETHERAI; // time in ms
    private int connectionTimeout = 5 * 60 * 1000;
    private int numRetry = 0;
    private int maxRetry = 10;
    private boolean checkJSON = true;
    private boolean checkJSONResponseContent = false;
    private Map<String, String> inMemoryCache = new HashMap<>(); // TODO: do we need to save it?

    public TogetherAIModel(String toghetherAiAPI, String modelName) {
        this.toghetherAiAPI = toghetherAiAPI;
        this.modelName = modelName;
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
        newMessage.setRole(USER);
        newMessage.setContent(prompt);
        String jsonRequest = getJsonForRequest();
        if (jsonRequest.isEmpty()) {
            log.trace("Return null because jsonRequest is empty: " + jsonRequest);
            return null;
        }
        String authorizationValue = "Bearer " + this.toghetherAiAPI;
        this.numRetry = 0;
        log.trace("Reset retry to 0");
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
                this.inputTokens += usage.getPromptTokens();
                this.outputTokens += usage.getCompletionTokens();
                log.trace("Add Assistant Message in getResponse: " + newMessage);
                this.messages.add(newMessage);
                this.messages.add(message);
            }
            return responseAPI;
        } catch (Exception e) {
            log.error("Exception with parsing response: " + response);
            log.error("Exception: " + e);
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
                mex.setRole(USER);
                mex.setContent(message.text());
                log.trace("Add User Message: " + mex.getContent());
                this.messages.add(mex);
            } else {
                Message mex = new Message();
                mex.setRole(ASSISTANT);
                mex.setContent(Mapper.toCleanJsonList(message.text()));
                //mex.setContent(message.text());
                log.trace("Add Assistant message: " + mex.getContent());
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
        if (this.numRetry >= this.maxRetry) {
            log.trace("Return null because reached max retry: " + this.numRetry + " over attempts " + this.maxRetry);
            return null;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(waitTime);
            URL url = URI.create(this.endPoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(this.connectionTimeout);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", authorizationValue);
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            JsonElement je = JsonParser.parseString​(jsonRequest);
//            String prettyJsonString = gson.toJson(je);
//            log.trace("Request: \n" + prettyJsonString);
//            log.trace("Is JSON valid: " + isValid(jsonRequest));
            byte[] input = jsonRequest.getBytes("utf-8");
            os.write(input, 0, input.length);
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseText = response.toString().trim();
            log.trace("Response: \n" + responseText);
            if (checkJSON && !Mapper.isJSON(responseText)) {
                log.trace("Return null because the response is not a JSON: \n" + responseText);
                return null;
            }
            connection.disconnect();
            return responseText;
        } catch (Exception e) {
            if (e instanceof IOException) {
                try {
                    if (isValid(jsonRequest) && this.numRetry > 1) return null;
                    log.trace("Retry the request IOE: " + this.numRetry);
                    log.trace("Exception: " + e);
                    TimeUnit.MILLISECONDS.sleep(waitTime);
                    this.numRetry++;
                    return makeRequest(jsonRequest, authorizationValue);
                } catch (InterruptedException ie) {
                    log.trace("Retry the request in catch");
                    this.numRetry++;
                }
            } else {
                log.error("Exception with the request: " + e);
                return null;
            }
        }
        return "";
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
                + "    \"messages\": {$MESSAGES$}\n"
                + "}";
        jsonReturn = jsonReturn.replace("{$MODEL_NAME$}", this.modelName);
        jsonReturn = jsonReturn.replace("{$MAX_TOKENS$}", this.maxTokens + "");
        jsonReturn = jsonReturn.replace("{$TEMPERATURE$}", this.temperature + "");
        jsonReturn = jsonReturn.replace("{$TOP_P$}", this.topP + "");
        try {
            String jsonMessages = objectMapper.writeValueAsString(this.messages);
            log.trace("Json Messages: " + jsonMessages);
            jsonReturn = jsonReturn.replace("{$MESSAGES$}", jsonMessages);
            log.trace("JsonReturn: \n " + jsonReturn);
            return jsonReturn.trim();
        } catch (JsonProcessingException jpe) {
            log.error("Error generating the json: " + jpe);
        }
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
