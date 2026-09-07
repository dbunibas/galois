package galois.llm.query;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import galois.llm.models.LocalChatModel;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;

import java.time.Duration;
import java.util.Map;

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

    public static ConversationalChain buildTogetherAIConversationalChain(String apiKey, String modelName, Boolean reasoningEnabled) {
        TogetherAIModel model = new TogetherAIModel(apiKey, modelName, TogetherAIConstants.STREAM_MODE, reasoningEnabled);
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

    public static ConversationalChain buildLocalConversationalChain(String baseUrl, String modelName) {
        ChatLanguageModel model = buildLocalChatLanguageModel(baseUrl, modelName);
        return ConversationalChain.builder().chatLanguageModel(model).build();
    }

    public static ChatLanguageModel buildLocalChatLanguageModel(String baseUrl, String modelName) {
        return LocalChatModel.localBuilder()
                .baseUrl(baseUrl)
                .apiKey(Configuration.getInstance().getLocalApiKey())
                .modelName(modelName)
                .temperature(Configuration.getInstance().getLocalTemperature())
                .maxTokens(Configuration.getInstance().getLocalMaxTokens())
                .reasoningEffort(Configuration.getInstance().getLocalReasoningEffort())
                .stream(Configuration.getInstance().getLocalStream())
                .chatTemplateKwargs(buildLocalChatTemplateKwargs())
                .build();
    }

    // Reasoning models render the thinking block through the chat template: switch it off unless the configuration asks for it
    private static Map<String, Object> buildLocalChatTemplateKwargs() {
        Boolean reasoningEnabled = Configuration.getInstance().getLocalReasoningEnabled();
        if (reasoningEnabled == null) return null;
        return Map.of("enable_thinking", reasoningEnabled);
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
