package floq;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import floq.llm.models.togetherai.TogetherAIConstants;
import org.apache.pdfbox.pdmodel.common.filespecification.PDSimpleFileSpecification;

public class Constants {

    public static final String EXECUTOR = "OLLAMA_EXECUTOR";
//    public static final String EXECUTOR = "TOGETHERAI_EXECUTOR"; //THIS REQUIRES A VALID TOGETHERAI_API
//    public static final String EXECUTOR = "OPENAI_EXECUTOR"; //THIS REQUIRES A VALID OPEN_AI_API_KEY

    //OLLAMA
    public static final String OLLAMA_MODEL = "llama3.1:8b";
    //    public static final String OLLAMA_MODEL = "llama3.1:70b";

    //TOGETHER AI
    public static final String TOGETHERAI_API = "<INSERT TOGETHER API>";
    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_70B;
    public static final int WAIT_TIME_MS_TOGETHERAI = 1000; // for tier-1 otherwise 1000 for unpaid tier

    //OPEN AI
    public static final String OPEN_AI_API_KEY = "<INSERT OPENAI API>";
    public static final OpenAiChatModelName OPEN_AI_CHAT_MODEL_NAME = OpenAiChatModelName.GPT_4_O_MINI;

    public static final String MODEL_LLAMA3 = "llama3";
    public static final String MODEL_GPT = "gpt";
    public static final String LLM_MODEL = Constants.MODEL_LLAMA3;

    public static final String EXPORT_EXCEL_PATH = "~/floq/test-results/";

    public static final boolean CACHE_ENABLED = false;
    public static final String CACHE_DIR = "";
}
