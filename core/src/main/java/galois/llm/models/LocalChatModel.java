package galois.llm.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import galois.utils.Mapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Chat model for OpenAI-compatible endpoints served locally (e.g. vLLM).
 * <p>
 * {@link OpenAiChatModel} builds its request through openai4j, whose request schema is closed: there is no way to add
 * the vendor specific parameters a local server understands. The most relevant one is {@code chat_template_kwargs},
 * needed to switch off thinking on reasoning models: without it a server with an active reasoning parser returns the
 * whole answer in the non-standard {@code reasoning} field, leaving {@code content} null, and
 * {@code OpenAiChatModel} fails with "text cannot be null".
 * <p>
 * This model keeps the {@link OpenAiChatModel} type (so it can be used wherever the plain OpenAI one is expected) but
 * writes the request body itself, adding {@code chat_template_kwargs} and {@code reasoning_effort}. As a safety net,
 * when the server still answers with a null {@code content} the reasoning field is used as the message text
 * (see {@link #useReasoningAsFallback}).
 */
@Slf4j
@Getter
public class LocalChatModel extends OpenAiChatModel implements IModel {

    private static final String DEFAULT_BASE_URL = "http://localhost:8000/v1/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final String chatCompletionsUrl;
    private final String apiKey;
    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;
    private final Integer seed;
    private final Duration timeout;
    private final int maxRetries;
    /** Extra kwargs for the chat template renderer, e.g. {@code {"enable_thinking": false}}. */
    private final Map<String, Object> chatTemplateKwargs;
    /** Server side reasoning budget: {@code none}, {@code low}, {@code medium}, {@code high}, ... */
    private final String reasoningEffort;
    /** When the server returns a null content, fall back to the reasoning field instead of failing. */
    private final boolean useReasoningAsFallback;

    @Builder(builderMethodName = "localBuilder", builderClassName = "LocalChatModelBuilder")
    public LocalChatModel(String baseUrl, String apiKey, String modelName, Double temperature, Integer maxTokens,
                          Integer seed, Duration timeout, Integer maxRetries, Map<String, Object> chatTemplateKwargs,
                          String reasoningEffort, Boolean useReasoningAsFallback, Boolean logRequests,
                          Boolean logResponses) {
        super(baseUrl == null ? DEFAULT_BASE_URL : baseUrl, apiKey, null, modelName, temperature, null, null, maxTokens,
                null, null, null, null, null, null, seed, null, null, null,
                timeout == null ? DEFAULT_TIMEOUT : timeout, maxRetries, null, logRequests, logResponses, null, null,
                null);
        String url = baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
        this.chatCompletionsUrl = (url.endsWith("/") ? url : url + "/") + "chat/completions";
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.seed = seed;
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.maxRetries = maxRetries == null ? DEFAULT_MAX_RETRIES : maxRetries;
        this.chatTemplateKwargs = chatTemplateKwargs;
        this.reasoningEffort = reasoningEffort;
        this.useReasoningAsFallback = useReasoningAsFallback == null || useReasoningAsFallback;
    }

    @Override
    public String text(String prompt) {
        Response<AiMessage> response = generate(List.of(UserMessage.from(prompt)));
        if (response == null || response.content() == null) return null;
        String text = response.content().text();
        return text == null ? null : text.trim();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String jsonRequest = getJsonForRequest(messages);
        log.trace("Request to {}: {}", chatCompletionsUrl, jsonRequest);
        String jsonResponse = makeRequest(jsonRequest);
        if (jsonResponse == null) {
            log.trace("Return null because the request to {} failed", chatCompletionsUrl);
            return null;
        }
        log.trace("Response: {}", jsonResponse);
        return parseResponse(jsonResponse);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        throw new UnsupportedOperationException("Tools are not supported by " + getClass().getSimpleName());
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        throw new UnsupportedOperationException("Tools are not supported by " + getClass().getSimpleName());
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        Response<AiMessage> response = generate(chatRequest.messages());
        if (response == null) return null;
        return ChatResponse.builder()
                .aiMessage(response.content())
                .tokenUsage(response.tokenUsage())
                .finishReason(response.finishReason())
                .build();
    }

    private String getJsonForRequest(List<ChatMessage> messages) {
        ObjectNode request = Mapper.MAPPER.createObjectNode();
        request.put("model", modelName);
        ArrayNode messagesNode = request.putArray("messages");
        for (ChatMessage message : messages) {
            ObjectNode messageNode = messagesNode.addObject();
            messageNode.put("role", getRole(message.type()));
            messageNode.put("content", message.text());
        }
        if (temperature != null) request.put("temperature", temperature);
        if (maxTokens != null) request.put("max_tokens", maxTokens);
        if (seed != null) request.put("seed", seed);
        if (reasoningEffort != null) request.put("reasoning_effort", reasoningEffort);
        if (chatTemplateKwargs != null && !chatTemplateKwargs.isEmpty()) {
            request.set("chat_template_kwargs", Mapper.MAPPER.valueToTree(chatTemplateKwargs));
        }
        return request.toString();
    }

    private String getRole(ChatMessageType type) {
        return switch (type) {
            case SYSTEM -> "system";
            case AI -> "assistant";
            default -> "user";
        };
    }

    private String makeRequest(String jsonRequest) {
        int numRetry = 0;
        while (numRetry < maxRetries) {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(chatCompletionsUrl).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout((int) timeout.toMillis());
                connection.setReadTimeout((int) timeout.toMillis());
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                if (apiKey != null) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    String error = connection.getErrorStream() == null ? ""
                            : IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
                    log.error("Request to {} failed with code {}: {}", chatCompletionsUrl, responseCode, error);
                    return null;
                }
                return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.trace("Request attempt number {} failed with exception: {}", numRetry, e.getMessage(), e);
                numRetry++;
            } catch (Exception e) {
                log.error("Generic Exception with the request. Skipping retry", e);
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private Response<AiMessage> parseResponse(String jsonResponse) {
        try {
            JsonNode root = Mapper.MAPPER.readTree(jsonResponse);
            JsonNode choice = root.path("choices").path(0);
            JsonNode message = choice.path("message");
            String text = getText(message);
            if (text == null) {
                log.error("No text in the response, both content and reasoning are null: {}", jsonResponse);
                return null;
            }
            JsonNode usage = root.path("usage");
            TokenUsage tokenUsage = new TokenUsage(usage.path("prompt_tokens").asInt(),
                    usage.path("completion_tokens").asInt());
            return Response.from(AiMessage.from(text), tokenUsage, getFinishReason(choice.path("finish_reason")));
        } catch (Exception e) {
            log.error("Exception with parsing response: " + jsonResponse, e);
            return null;
        }
    }

    private String getText(JsonNode message) {
        JsonNode content = message.path("content");
        if (!content.isNull() && !content.isMissingNode()) return content.asText();
        if (!useReasoningAsFallback) return null;
        // Reasoning models behind an active reasoning parser answer here, leaving content null
        for (String field : List.of("reasoning", "reasoning_content")) {
            JsonNode reasoning = message.path(field);
            if (!reasoning.isNull() && !reasoning.isMissingNode()) {
                log.trace("Content is null, using the {} field as message text", field);
                return reasoning.asText();
            }
        }
        return null;
    }

    private FinishReason getFinishReason(JsonNode finishReason) {
        return switch (finishReason.asText("")) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls", "function_call" -> FinishReason.TOOL_EXECUTION;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.OTHER;
        };
    }
}
