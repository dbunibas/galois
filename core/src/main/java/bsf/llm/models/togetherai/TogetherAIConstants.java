package bsf.llm.models.togetherai;

public class TogetherAIConstants {
    //    public static final String MODEL_LLAMA3_8B = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo";
    public static final String MODEL_LLAMA3_8B = "meta-llama/Llama-3-8b-chat-hf";
    public static final String MODEL_LLAMA3_70B = "meta-llama/Llama-3-70b-chat-hf";
    public static final String MODEL_LLAMA3_1_8B = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo";
    public static final String MODEL_LLAMA3_1_70B = "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo";
//    public static final String MODEL_LLAMA3_3_70B = "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free";
    public static final String MODEL_LLAMA3_3_70B = "meta-llama/Llama-3.3-70B-Instruct-Turbo";
    public static final String MODEL_LLAMA4_SCOUT = "meta-llama/Llama-4-Scout-17B-16E-Instruct";
//    public static final String MODEL_DEEPSEEK_R1_DISTIL_LLAMA_70B = "deepseek-ai/DeepSeek-R1-Distill-Llama-70B-free";
    public static final String MODEL_DEEPSEEK_R1_DISTIL_LLAMA_70B = "deepseek-ai/DeepSeek-R1-Distill-Llama-70B";
    public static final String MODEL_QWEN_2_5_7B = "Qwen/Qwen2.5-7B-Instruct-Turbo";
    public static final String MODEL_QWEN3_235B = "Qwen/Qwen3-235B-A22B-fp8-tput";
    public static final String MODEL_MISTRAL_0_3_7B = "mistralai/Mistral-7B-Instruct-v0.3";
    public static final String MODEL_GEMMA_2_9B = "google/gemma-2-9b-it";
    public static final String MODEL_KIMI_K2 = "moonshotai/Kimi-K2-Instruct-0905";
    public static final Integer MAX_TOKENS = 8194; // for other, for gemma 8194

    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String BASE_ENDPOINT = "https://api.together.xyz/v1/";

    public static final boolean STREAM_MODE = true; // Set to false for Probabilities, otherwise set it to true

//    public static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
//    public static final int CONNECTION_TIMEOUT = 1 * 10 * 1000;
    public static final int CONNECTION_TIMEOUT = 1 * 1 * 1000;
//    public static final int MAX_RETRY = 10;
    public static final int MAX_RETRY = 2;
}
