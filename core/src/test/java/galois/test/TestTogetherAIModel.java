package galois.test;

import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestTogetherAIModel {

    private String API_KEI = Configuration.getInstance().getTogetheraiApiKey();

    @Test
    public void testRequestText() {
        TogetherAIModel toghetherAiModel = new TogetherAIModel(this.API_KEI, TogetherAIConstants.MODEL_LLAMA3_8B, false);
        String response = toghetherAiModel.generate("Given the following query, populate the table with actual values.\n" +
                "query: select population and city_name from usa_city where population > 150000.\n" +
                "Respond with JSON only. Don't add any comment.\n" +
                "Use the following JSON schema:\n" +
                "{\"title\":\"usa_city\",\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"city_name\":{\"title\":\"city_name\",\"type\":\"string\"},\"population\":{\"title\":\"population\",\"type\":\"integer\"}}}}");
        log.info("Response: {}", response);
    }

}
