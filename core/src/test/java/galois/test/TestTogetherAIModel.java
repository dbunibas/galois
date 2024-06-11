package galois.test;

import galois.llm.models.IModel;
import galois.llm.models.TogetherAIModel;
import org.junit.jupiter.api.Test;

public class TestTogetherAIModel {

    private String API_KEI = "YOUR_TOGETHER_API_KEY";

    @Test
    public void testRequest() {
        IModel toghetherAiModel = new TogetherAIModel(this.API_KEI, TogetherAIModel.MODEL_LLAMA3_8B);
        String response = toghetherAiModel.text("ciao, scrivimi qualcosa di simpatico");
        System.out.println("Response: " + response);
        String response2 = toghetherAiModel.text("puoi scrivermi qualcosa di più simpatico?. Non produrre poesie, ma racconta una barzelletta");
        System.out.println("Response: " + response2);
    }

}
