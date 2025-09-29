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
//    public static final String OPEN_AI_CHAT_MODEL_NAME = OpenAiChatModelName.GPT_4_O_MINI.toString();
//    public static final String OPEN_AI_CHAT_MODEL_NAME = "gpt-4.1-mini";
    public static final String OPEN_AI_CHAT_MODEL_NAME = "gpt-4.1";
    public static final String GEMINI_CHAT_MODEL_NAME = "gemini-2.5-flash-lite";
//    public static final String GEMINI_CHAT_MODEL_NAME = "gemini-2.5-flash";
    
    public static final int WAIT_TIME_MS_TOGETHERAI = 40; // for tier-2 40 for tier-1 110 otherwise 1000 for unpaid tier
//    public static final int WAIT_TIME_MS_TOGETHERAI = 1000; // for tier-2 40 for tier-1 110 otherwise 1000 for unpaid tier
//    public static final int WAIT_TIME_MS_TOGETHERAI = 6000; // for tier-2 40 for tier-1 110 otherwise 1000 for unpaid tier
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_GEMMA_2_9B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_DEEPSEEK_R1_DISTIL_LLAMA_70B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_QWEN3_235B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_3_70B;
    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_8B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_QWEN_2_5_7B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_MISTRAL_0_3_7B;
    

//    public static final String OLLAMA_MODEL = "llama3.1:8b";
    public static final String OLLAMA_MODEL = "llama3.1:70b";

    public static final String MODEL_LLAMA3 = "llama3";
    public static final String MODEL_LLAMA4 = "llama4";
    public static final String MODEL_GPT = "gpt";
    public static final String MODEL_DEEPSEEK = "deepseek";
    public static final String MODEL_QWEN = "qwen";
    public static final String MODEL_MISTRAL = "mistral";
    public static final String MODEL_GEMMA = "gemma";

    public static final String LLM_MODEL = Constants.MODEL_LLAMA3;

    public static final boolean CACHE_ENABLED = true;
//    public static final String CACHE_DIR = "/Users/enzoveltri/git/galois/core/src/test/resources/llm-bench/cache/DeepSeek-R1-Distill-Llama-70B-free/";
//    public static final String CACHE_DIR = "/Users/enzoveltri/git/galois/core/src/test/resources/llm-bench/cache/Qwen2.5-7B-Instruct-Turbo/";
    public static final String CACHE_DIR = "/Users/enzoveltri/git/galois/core/src/test/resources/llm-bench/cache/gemma-2-9b-it/";
}
