package galois.test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import galois.llm.models.LocalChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestCustomEndpoint {
    @Test
    public void testCustomOpenAiEndpoint() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey("local")
                .modelName("qwen35-2b")
                .baseUrl("http://localhost:8003/v1/")
                .temperature(0.0)
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .build();
        assertNotNull(localModel);

        String response = localModel.generate("Return the plain text \"Hello World\" without quotes.");
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertEquals("Hello World", response);
    }

    @Test
    public void testCustomOpenAiEndpointTokens() throws IOException {
        LocalChatModel localModel = LocalChatModel.localBuilder()
                .apiKey("local")
                .modelName("qwen35-2b")
                .baseUrl("http://localhost:8003/v1/")
                .temperature(0.0)
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


}
