package galois;

import galois.llm.models.togetherai.TogetherAIConstants;

public class Constants {
    public static final String LLM_MODEL = Constants.MODEL_LLAMA3;

    // ----- TOGETHER AI CONFIG --------
    public static final String TOGETHERAI_API = "<YOUR_API_KEY>";
    public static final int WAIT_TIME_MS_TOGETHERAI = 1000; // for tier-2 40 for tier-1 110 otherwise 1000 for unpaid tier
    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_8B;

    // ----- OPEN AI CONFIG --------
    public static final String OPEN_AI_API_KEY = "<YOUR_API_KEY>";
    public static final String OPEN_AI_CHAT_MODEL_NAME = "gpt-4.1";

    // ----- GEMINI CONFIG --------
    public static final String GEMINI_API_KEY = "<YOUR_API_KEY>";
    public static final String GEMINI_CHAT_MODEL_NAME = "gemini-2.5-flash-lite";

    // ----- OTHER CONFIG --------
    public static final String OLLAMA_MODEL = "llama3.1:70b";

    public static final String MODEL_LLAMA3 = "llama3";
    public static final String MODEL_LLAMA4 = "llama4";
    public static final String MODEL_GPT = "gpt";
    public static final String MODEL_DEEPSEEK = "deepseek";
    public static final String MODEL_QWEN = "qwen";
    public static final String MODEL_MISTRAL = "mistral";
    public static final String MODEL_GEMMA = "gemma";

    public static final String MODEL_KIMI = "kimi";

    public static final boolean CACHE_ENABLED = false;
    public static final String CACHE_DIR = "/Temp/cache";

    public static final String EXPORT_EXCEL_PATH = "~/test-results/";
}