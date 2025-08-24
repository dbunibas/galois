package galois.llm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.util.stream.Collectors;

@Slf4j
@Builder
public class TogetherAIEmbeddingModel extends DimensionAwareEmbeddingModel {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final String toghetherAiAPI;
    private final String modelName;
    @Builder.Default
    private Map<String, String> inMemoryCache = new HashMap<>();

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<String> input = textSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(modelName)
                .input(input)
                .build();
        String jsonRequest = getJsonForRequest(request);
        String authorizationValue = "Bearer " + this.toghetherAiAPI;
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
            EmbeddingResponse embeddingResponse = objectMapper.readValue(response, EmbeddingResponse.class);
            List<Embedding> embeddings = embeddingResponse.data
                    .stream()
                    .map(er -> er.embedding)
                    .map(Embedding::from)
                    .collect(Collectors.toList());
            return Response.from(embeddings);
        } catch (JsonProcessingException e) {
            log.error("Unable to read embeddings", e);
            throw new RuntimeException(e);
        }

    }

    private String makeRequest(String jsonRequest, String authorizationValue) {
        int numRetry = 0;
        while (numRetry < TogetherAIConstants.MAX_RETRY) {
            HttpURLConnection connection = null;
            try {
                TimeUnit.MILLISECONDS.sleep(Configuration.getInstance().getTogetheraiWaitTimeMs());
                URL url = URI.create(TogetherAIConstants.BASE_ENDPOINT + "embeddings").toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TogetherAIConstants.CONNECTION_TIMEOUT);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", authorizationValue);
                connection.setDoOutput(true);
                String responseText = getString(jsonRequest, connection);
                log.trace("Response: \n{}", responseText);
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

    private static @NotNull String getString(String jsonRequest, HttpURLConnection connection) throws IOException {
        OutputStream os = connection.getOutputStream();
        byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
        os.flush();
        connection.connect();
        if (connection.getResponseCode() != 200) {
            log.error("Error response: {}\nRequest: {}", IOUtils.toString(new InputStreamReader(connection.getErrorStream())), jsonRequest);
            throw new IOException("Error response " + connection.getErrorStream());
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return response.toString().trim();
    }

    private String getJsonForRequest(EmbeddingRequest request) {
        try {
            log.info("Embedding request: {}", request);
            String jsonMessages = objectMapper.writeValueAsString(request);
            log.trace("Json Request: {}", jsonMessages);
            return jsonMessages.trim();
        } catch (JsonProcessingException e) {
            log.error("Unable to generate json string", e);
            throw new RuntimeException(e);
        }
    }

    @Builder
    @Data
    static class EmbeddingRequest {

        private String model;
        private List<String> input;

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingResponse {
        public String model;
        public String object;
        public ArrayList<EmbeddingResponseVector> data;
    }

    @Data
    @NoArgsConstructor
    static class EmbeddingResponseVector {
        public int index;
        public String object;
        public ArrayList<Float> embedding;
    }


}
