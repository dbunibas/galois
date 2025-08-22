package galois.llm.query;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;

import java.time.Duration;

public class ConversationalChainFactory {

    public static Chain<String, String> buildOllamaLlama3ConversationalChain() {
        return buildOllamaConversationalChain(Configuration.getInstance().getOllamaModel());
    }

    public static Chain<String, String> buildOllamaMistralConversationalChain() {
        return buildOllamaConversationalChain("mistral");
    }

    public static Chain<String, String> buildOllamaPhi3ConversationalChain() {
        return buildOllamaConversationalChain("phi3");
    }

    public static ChatLanguageModel buildOllamaLlama3ChatLanguageModel() {
        return buildOllamaChatLangageModel(Configuration.getInstance().getOllamaModel());
    }

    public static ChatLanguageModel buildOllamaMistralChatLanguageModel() {
        return buildOllamaChatLangageModel("mistral");
    }

    public static ChatLanguageModel buildOllamaPhi3ChatLanguageModel() {
        return buildOllamaChatLangageModel("phi3");
    }

    public static ConversationalChain buildTogetherAIConversationalChain(String apiKey, String modelName) {
        TogetherAIModel model = new TogetherAIModel(apiKey, modelName, TogetherAIConstants.STREAM_MODE);
        return ConversationalChain.builder().chatLanguageModel(model).build();
    }

    public static ChatLanguageModel buildTogetherAiChatLanguageModel(String apiKey, String modelName) {
        return new TogetherAIModel(apiKey, modelName, TogetherAIConstants.STREAM_MODE);
    }

    public static ConversationalChain buildOpenAIConversationalChain(String apiKey, String modelName) {
        ChatLanguageModel model = buildOpenAIChatLanguageModel(apiKey, modelName);
        return ConversationalChain.builder().chatLanguageModel(model).build();
    }

    public static ChatLanguageModel buildOpenAIChatLanguageModel(String apiKey, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    private static ChatLanguageModel buildOllamaChatLangageModel(String modelName) {
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(Configuration.getInstance().getOllamaUrl())
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofMinutes(5))
                .build();
        return chatModel;
    }

    private static Chain<String, String> buildOllamaConversationalChain(String modelName) {
        ChatLanguageModel chatModel = buildOllamaChatLangageModel(modelName);
        return ConversationalChain.builder().chatLanguageModel(chatModel).build();
    }


}
