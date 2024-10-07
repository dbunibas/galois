package galois.llm.models.togetherai;

import galois.Constants;

public class TogetherAIConstants {
    //    public static final String MODEL_LLAMA3_8B = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo";
    public static final String MODEL_LLAMA3_8B = "meta-llama/Llama-3-8b-chat-hf";
    public static final String MODEL_LLAMA3_70B = "meta-llama/Llama-3-70b-chat-hf";
    public static final String MODEL_LLAMA3_1_8B = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo";
    public static final String MODEL_LLAMA3_1_70B = "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo";

    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String BASE_ENDPOINT = "https://api.together.xyz/v1/";


    public static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
    public static final int MAX_RETRY = 10;
//    public static final int MAX_RETRY = 1;
}
