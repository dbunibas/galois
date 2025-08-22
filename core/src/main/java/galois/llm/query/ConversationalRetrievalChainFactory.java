package galois.llm.query;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;

import java.time.Duration;

import static galois.llm.query.ConversationalChainFactory.buildOpenAIChatLanguageModel;

public class ConversationalRetrievalChainFactory {

    public static Chain<String, String> buildOllamaLlama3ConversationalRetrivalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain(Configuration.getInstance().getOllamaModel(), contentRetriever);
    }

    public static Chain<String, String> buildOllamaMistralConversationalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain("mistral", contentRetriever);
    }

    public static Chain<String, String> buildOllamaPhi3ConversationalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain("phi3", contentRetriever);
    }

    public static ChatLanguageModel buildOllamaLlama3ChatLanguageModel(ContentRetriever contentRetriever) {
        return buildOllamaChatLangageModel(Configuration.getInstance().getOllamaModel());
    }

    public static ChatLanguageModel buildOllamaMistralChatLanguageModel(ContentRetriever contentRetriever) {
        return buildOllamaChatLangageModel("mistral");
    }

    public static ChatLanguageModel buildOllamaPhi3ChatLanguageModel(ContentRetriever contentRetriever) {
        return buildOllamaChatLangageModel("phi3");
    }

    public static Chain<String, String> buildTogetherAIConversationalRetrivalChain(String apiKey, String modelName, ContentRetriever contentRetriever) {
        TogetherAIModel model = new TogetherAIModel(apiKey, modelName, TogetherAIConstants.STREAM_MODE);
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .build();
        //TODO: Improve retrieval for prompts > 0. IDEA: Inject different values from first prompt
//        DefaultRetrievalAugmentor defaultRetrievalAugmentor = DefaultRetrievalAugmentor.builder().contentRetriever(contentRetriever)
//                .contentInjector(DefaultContentInjector.builder()
//                        .promptTemplate(toPromptTemplateWithNewVariableNames(null))
//                        .build()).build();
//        return new CustomConversionalRetrievalChain(model, contentRetriever, defaultRetrievalAugmentor);
    }

    private static PromptTemplate toPromptTemplateWithNewVariableNames(PromptTemplate oldPromptTemplate) {
        return oldPromptTemplate != null ? PromptTemplate.from(oldPromptTemplate.template().replaceAll("\\{\\{question}}", "{{userMessage}}").replaceAll("\\{\\{information}}", "{{contents}}")) : PromptTemplate.from("Answer the following question to the best of your ability: {{userMessage}}\n\nBase your answer on the following information:\n{{contents}}");
    }

    public static Chain<String, String> buildOpenAIConversationalRetrievalChain(String apiKey, String modelName, ContentRetriever contentRetriever) {
        ChatLanguageModel model = buildOpenAIChatLanguageModel(apiKey, modelName);
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .build();
    }

    public static ChatLanguageModel buildTogetherAiChatLanguageModel(String apiKey, String modelName, ContentRetriever contentRetriever) {
        return new TogetherAIModel(apiKey, modelName, TogetherAIConstants.STREAM_MODE);
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

    private static Chain<String, String> buildOllamaConversationalChain(String modelName, ContentRetriever contentRetriever) {
        ChatLanguageModel chatModel = buildOllamaChatLangageModel(modelName);
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }


}
