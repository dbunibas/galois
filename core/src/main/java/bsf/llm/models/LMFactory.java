package bsf.llm.models;

import dev.langchain4j.model.chat.ChatLanguageModel;
import bsf.Constants;
import bsf.llm.models.togetherai.TogetherAIConstants;
import bsf.llm.query.ConversationalChainFactory;

public class LMFactory {

    public static ChatLanguageModel getLMModel(){
        ChatLanguageModel model;
        if (Constants.LLM_MODEL.equals(Constants.MODEL_LLAMA3)) {
             model = new TogetherAIModel(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL, TogetherAIConstants.STREAM_MODE);
        }else if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) {
            model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        }else {
            throw new IllegalArgumentException("Unknown model " + Constants.LLM_MODEL);
        }
        return model;
    }

}
