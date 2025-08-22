package galois.llm.models;

import dev.langchain4j.model.chat.ChatLanguageModel;
import galois.Constants;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.ConversationalChainFactory;
import galois.utils.Configuration;

public class LMFactory {

    public static ChatLanguageModel getLMModel() {
        ChatLanguageModel model;
        if (Configuration.getInstance().getLLMModel().equals(Constants.MODEL_LLAMA3)) {
            model = new TogetherAIModel(Configuration.getInstance().getTogetheraiApiKey(), Configuration.getInstance().getTogetheraiModel(), TogetherAIConstants.STREAM_MODE);
        } else if (Configuration.getInstance().getLLMModel().equals(Constants.MODEL_GPT)) {
            model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Configuration.getInstance().getOpenaiApiKey(), Configuration.getInstance().getOpenaiModelName());
        } else {
            throw new IllegalArgumentException("Unknown model " + Configuration.getInstance().getLLMModel());
        }
        return model;
    }

}
