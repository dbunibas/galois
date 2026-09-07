package galois.test;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import galois.llm.models.LocalChatModel;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static galois.llm.query.ConversationalChainFactory.buildLocalConversationalChain;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestCustomEndpoint {
    @Test
    public void testCustomOpenAiEndpoint() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .temperature(Configuration.getInstance().getLocalTemperature())
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .build();
        assertNotNull(localModel);

        String response = localModel.generate("Return the plain text \"Hello World\" without quotes.");
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertEquals("Hello World", response.trim());
    }

    @Test
    public void testCustomOpenAiEndpointPrompt() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .temperature(Configuration.getInstance().getLocalTemperature())
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .build();
        assertNotNull(localModel);

        String response = localModel.generate("How many counties are there in Virginia State?");
        assertNotNull(response);
        log.info("Response: {}", response);
    }

    @Test
    public void testCustomOpenAiEndpointTokens() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .temperature(Configuration.getInstance().getLocalTemperature())
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .build();
        assertNotNull(localModel);
        ChatMessage message = UserMessage.from("Return the plain text \"Hello World\" without quotes.");
        Response<AiMessage> response = localModel.generate(message);

        log.info("{}", response);
        assertNotNull(response);
        assertNotNull(response.content());
        assertNotNull(response.tokenUsage());
        assertTrue(response.tokenUsage().inputTokenCount() > 0);
        assertTrue(response.tokenUsage().outputTokenCount() > 0);
        assertTrue(response.tokenUsage().totalTokenCount() > 0);
    }

    @Test
    public void testCustomOpenAiEndpointStream() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .temperature(Configuration.getInstance().getLocalTemperature())
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .stream(true)
                .build();
        assertNotNull(localModel);
        ChatMessage message = UserMessage.from("Return the plain text \"Hello World\" without quotes.");
        Response<AiMessage> response = localModel.generate(message);

        log.info("{}", response);
        assertNotNull(response);
        assertNotNull(response.content());
        assertEquals("Hello World", response.content().text().trim());
        // The chunks are reassembled, so the usage must be reported exactly as in the non streaming mode
        assertNotNull(response.tokenUsage());
        assertTrue(response.tokenUsage().inputTokenCount() > 0);
        assertTrue(response.tokenUsage().outputTokenCount() > 0);
    }

    @Test
    public void testMaxTokensCapsTheAnswer() {
        int maxTokens = 16;
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .temperature(Configuration.getInstance().getLocalTemperature())
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .maxTokens(maxTokens)
                .stream(true)
                .build();
        assertNotNull(localModel);
        Response<AiMessage> response = localModel.generate(UserMessage.from("Write a long essay about databases."));

        // The cap is a server side sampling parameter: streaming has to report it exactly as the blocking mode does
        assertNotNull(response.tokenUsage());
        assertEquals(maxTokens, response.tokenUsage().outputTokenCount());
        assertEquals(FinishReason.LENGTH, response.finishReason());
        // The chunk carrying the finish reason also carries the last token: it must not be dropped
        assertFalse(response.content().text().isBlank());
    }

    @Test
    public void testUnreachableEndpointReturnsEmptyResponse() {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(Configuration.getInstance().getLocalModelName())
                .baseUrl("http://localhost:9/v1/") // nothing listens here, so every attempt is refused
                .maxRetries(1)
                .build();
        assertNotNull(localModel);
        Response<AiMessage> response = localModel.generate(UserMessage.from("Any prompt will do."));

        // A failed request is an empty answer, not a null one
        assertNotNull(response);
        assertNotNull(response.content());
        assertEquals("", response.content().text());
        assertEquals("", localModel.text("Any prompt will do."));
    }

    @Test
    public void testFailingEndpointReturnsEmptyResponse() {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName("a-model-the-server-does-not-serve")
                .baseUrl(Configuration.getInstance().getLocalBaseUrl())
                .maxRetries(1)
                .build();
        assertNotNull(localModel);
        Response<AiMessage> response = localModel.generate(UserMessage.from("Any prompt will do."));

        assertNotNull(response);
        assertNotNull(response.content());
        assertEquals("", response.content().text());
    }

    @Test
    public void testChainActuallyChains() {
        ConversationalChain chain = buildLocalConversationalChain(
                Configuration.getInstance().getLocalBaseUrl(),
                Configuration.getInstance().getLocalModelName()
        );

        String response = chain.execute("Report three valid Python keywords");
        assertNotNull(response);

        String next = chain.execute("Other three");
        assertNotNull(next);
    }

}
