package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import galois.llm.models.TogetherAIModel;

import java.time.Duration;

public class ConversationalChainFactory {

    public static ConversationalChain buildOllamaLlama3ConversationalChain() {
        return buildOllamaConversationalChain("llama3");
    }

    public static ConversationalChain buildOllamaMistralConversationalChain() {
        return buildOllamaConversationalChain("mistral");
    }

    public static ConversationalChain buildOllamaPhi3ConversationalChain() {
        return buildOllamaConversationalChain("phi3");
    }
    
    public static ChatLanguageModel buildOllamaLlama3ChatLanguageModel() {
        return buildOllamaChatLangageModel("llama3");
    }

    public static ChatLanguageModel buildOllamaMistralChatLanguageModel() {
        return buildOllamaChatLangageModel("mistral");
    }

    public static ChatLanguageModel buildOllamaPhi3ChatLanguageModel() {
        return buildOllamaChatLangageModel("phi3");
    }

    public static ConversationalChain buildTogetherAIConversationalChain(String apiKey, String modelName) {
        TogetherAIModel model = new TogetherAIModel(apiKey, modelName);
        return ConversationalChain.builder().chatLanguageModel(model).build();
    }
    
    public static ChatLanguageModel buildTogetherAiChatLanguageModel(String apiKey, String modelName) {
        return new TogetherAIModel(apiKey, modelName);
    }
    
    private static ChatLanguageModel buildOllamaChatLangageModel(String modelName) {
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofMinutes(5))
                .build();
        return chatModel;
    }

    private static ConversationalChain buildOllamaConversationalChain(String modelName) {
        ChatLanguageModel chatModel = buildOllamaChatLangageModel(modelName);
        return ConversationalChain.builder().chatLanguageModel(chatModel).build();
    }
    
    
}
