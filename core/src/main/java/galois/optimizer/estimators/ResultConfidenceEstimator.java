package galois.optimizer.estimators;

import dev.langchain4j.model.chat.ChatLanguageModel;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.ConversationalChainFactory;
import galois.utils.Configuration;
import galois.utils.Mapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ResultConfidenceEstimator {

    public Double estimateConfidence(String question, List<String> tuples) {
        String prompt = """
                You have just generated the following answer to a given question.
                
                Question: \s
                {question}
                
                Your answer: \s
                {answer}
                
                Now, evaluate your own answer and provide a confidence score between 0% and 100%, indicating how confident you are in the accuracy and reliability of your response.
                
                Respond in the following JSON format: \s
                {"confidence_score": number}
                """;
        prompt = prompt.replace("{question}", question);
        prompt = prompt.replace("{answer}", String.join("\n", tuples));
        ChatLanguageModel model = new TogetherAIModel(Configuration.getInstance().getTogetheraiApiKey(), Configuration.getInstance().getTogetheraiModel(), TogetherAIConstants.STREAM_MODE);
        if (Configuration.getInstance().getLLMModel().equals(Constants.MODEL_GPT))
            model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Configuration.getInstance().getOpenaiApiKey(), Configuration.getInstance().getOpenaiModelName());
        String response = model.generate(prompt);
        log.trace("Confidence estimator prompt: {}\nResponse: {}", prompt, response);
        Map<String, Object> answerMap = Mapper.fromJsonToMap(response);
        if (answerMap == null || answerMap.size() != 1) {
            return null;
        }
        return Double.valueOf(answerMap.values().iterator().next() + "");
    }

}
