package floq.llm.query;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import floq.llm.models.TogetherAIModel;
import floq.llm.models.togetherai.TogetherAIConstants;

import java.time.Duration;

import static floq.Constants.OLLAMA_MODEL;
import static floq.llm.query.ConversationalChainFactory.buildOpenAIChatLanguageModel;

public class ConversationalRetrievalChainFactory {

    public static Chain<String, String> buildOllamaLlama3ConversationalRetrivalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain(OLLAMA_MODEL, contentRetriever);
    }

    public static Chain<String, String> buildOllamaMistralConversationalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain("mistral", contentRetriever);
    }

    public static Chain<String, String> buildOllamaPhi3ConversationalChain(ContentRetriever contentRetriever) {
        return buildOllamaConversationalChain("phi3", contentRetriever);
    }

    public static ChatLanguageModel buildOllamaLlama3ChatLanguageModel(ContentRetriever contentRetriever) {
        return buildOllamaChatLangageModel(OLLAMA_MODEL);
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

    public static Chain<String, String> buildOpenAIConversationalRetrievalChain(String apiKey, OpenAiChatModelName modelName, ContentRetriever contentRetriever) {
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
                .baseUrl("http://127.0.0.1:11434")
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
