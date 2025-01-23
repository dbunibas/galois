package galois;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import galois.llm.models.togetherai.TogetherAIConstants;

public class Constants {
    // TODO: move to properties

//    public static final String TOGETHERAI_API = "YOUR_API_KEY";
    public static final String TOGETHERAI_API = "YOUR_API_KEY";
    public static final String EXPORT_EXCEL_PATH = "YOUR_PATH";

    public static final String OPEN_AI_API_KEY = "YOUR_API_KEY";
    public static final OpenAiChatModelName OPEN_AI_CHAT_MODEL_NAME = OpenAiChatModelName.GPT_4_O_MINI;
    public static final int WAIT_TIME_MS_TOGETHERAI = 40; // for tier-2 40 for tier-1 110 otherwise 1000 for unpaid tier
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_8B;
    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_70B;

//    public static final String OLLAMA_MODEL = "llama3.1:8b";
    public static final String OLLAMA_MODEL = "llama3.1:70b";
    

    public static final String MODEL_LLAMA3 = "llama3";
    public static final String MODEL_GPT = "gpt";
    public static final String LLM_MODEL = Constants.MODEL_LLAMA3;

    public static final boolean CACHE_ENABLED = false;
    public static final String CACHE_DIR = "";
}
