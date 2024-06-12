package galois.test;

import galois.Constants;
import galois.llm.models.IModel;
import galois.llm.models.TogetherAIModel;
import org.junit.jupiter.api.Test;

public class TestTogetherAIModel {

    private String API_KEI = Constants.TOGETHERAI_API;

    @Test
    public void testRequestText() {
        IModel toghetherAiModel = new TogetherAIModel(this.API_KEI, TogetherAIModel.MODEL_LLAMA3_8B);
        String response = toghetherAiModel.text("ciao, scrivimi qualcosa di simpatico");
        System.out.println("Response: " + response);
        String response2 = toghetherAiModel.text("puoi scrivermi qualcosa di più simpatico?. Non produrre poesie, ma racconta una barzelletta");
        System.out.println("Response: " + response2);
    }

}
