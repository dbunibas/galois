package floq.llm;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;

public class TokensEstimator {

    private ModelType modelType = ModelType.GPT_4;
    private EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public TokensEstimator() {
    }

    public TokensEstimator(ModelType modelType) {
        this.modelType = modelType;
    }

    public double getTokens(String message) {
        Encoding enc = registry.getEncodingForModel(modelType);
        double inputToken = enc.encode(message).size();
        return inputToken;
    }

}
