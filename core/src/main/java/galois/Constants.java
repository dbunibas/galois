package galois;

import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import org.apache.pdfbox.pdmodel.common.filespecification.PDSimpleFileSpecification;

public class Constants {
 // TODO: move to properties
    public static final String TOGETHERAI_API = "YOUR_API_KEY";    
    public static final String EXPORT_EXCEL_PATH = "/Users/donatello/Projects/research/galois/test-results/";

    public static final String OPEN_AI_API_KEY = "";
    public static final OpenAiChatModelName OPEN_AI_CHAT_MODEL_NAME = OpenAiChatModelName.GPT_4;
    public static final int WAIT_TIME_MS_TOGETHERAI = 110; // for tier-1 otherwise 1000 for unpaid tier
    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_8B;
//    public static final String TOGETHERAI_MODEL = TogetherAIConstants.MODEL_LLAMA3_1_70B;

    public static final String OLLAMA_MODEL = "llama3.1:8b";
//    public static final String OLLAMA_MODEL = "llama3.1:70b";
}
