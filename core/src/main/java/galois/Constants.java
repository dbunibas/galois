package galois;

import galois.llm.models.TogetherAIModel;

public class Constants {
 // TODO: move to properties
    public static final String TOGETHERAI_API = "YOUR_API_KEY";    
    public static final int WAIT_TIME_MS_TOGETHERAI = 110; // for tier-1 otherwise 1000 for unpaid tier
    public static final String EXPORT_EXCEL_PATH = "/Users/enzoveltri/Desktop/galois/";
    public static final String TOGETHERAI_MODEL = TogetherAIModel.MODEL_LLAMA3_1_70B;
}
