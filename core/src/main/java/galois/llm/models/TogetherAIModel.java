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
import galois.llm.models.togetherai.Choice;
import galois.llm.models.togetherai.Message;
import galois.llm.models.togetherai.ResponseTogetherAI;
import galois.llm.models.togetherai.Usage;

@Slf4j
public class TogetherAIModel implements IModel {

    public static final String MODEL_LLAMA3_8B = "meta-llama/Llama-3-8b-chat-hf";
    public static final String MODEL_LLAMA3_70B = "meta-llama/Llama-3-70b-chat-hf";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    private String endPoint = "https://api.together.xyz/v1/chat/completions";
    private String toghetherAiAPI;
    private String modelName;
    private int maxTokens = 512;
    private double temperature = 0.7;
    private List<Message> messages = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private int inputTokens = 0;
    private int outputTokens = 0;

    public TogetherAIModel(String toghetherAiAPI, String modelName) {
        this.toghetherAiAPI = toghetherAiAPI;
        this.modelName = modelName;
    }

    @Override
    public String text(String prompt) {
        Message newMessage = new Message();
        newMessage.setRole(USER);
        newMessage.setContent(prompt);
        this.messages.add(newMessage);
        String jsonRequest = getJsonForRequest();
        if (jsonRequest.isEmpty()) {
            return null;
        }
        String authorizationValue = "Bearer " + this.toghetherAiAPI;
        String response = this.makeRequest(jsonRequest, authorizationValue);
        try {
            ResponseTogetherAI responseAPI = objectMapper.readValue(response, ResponseTogetherAI.class);
            Choice choice = responseAPI.getChoices().get(0);
            Message message = choice.getMessage();
            if (message != null) {
                Usage usage = responseAPI.getUsage();
                this.inputTokens += usage.getPromptTokens();
                this.outputTokens += usage.getCompletionTokens();
                this.messages.add(message);
                return message.getContent().trim();
            }
        } catch (Exception e) {
            log.error("Exception with parsing response: " + response);
            log.error("Exception: " + e);
        }
        return null;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    private String makeRequest(String jsonRequest, String authorizationValue) {
        try {
            URL url = URI.create(this.endPoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", authorizationValue);
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            byte[] input = jsonRequest.getBytes("utf-8");
            os.write(input, 0, input.length);
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString().trim();
        } catch (Exception e) {
            log.error("Exception with the request: " + e);
        }
        return "";
    }

    private String getJsonForRequest() {
        String jsonReturn = "{\n"
                + "    \"model\": \"{$MODEL_NAME$}\",\n"
                + "    \"max_tokens\": {$MAX_TOKENS$},\n"
                + "    \"temperature\": {$TEMPERATURE$},\n"
                + "    \"top_p\": 0.7,\n"
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

}