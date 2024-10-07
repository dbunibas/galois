package galois.optimizer.estimators;

import galois.llm.models.IModel;
import galois.llm.models.OllamaModel;
import galois.prompt.EEstimatorPrompt;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.ITable;
import speedy.model.expressions.Expression;

import java.util.Map;

import static galois.Constants.OLLAMA_MODEL;
import static galois.utils.Mapper.fromJsonToMap;

@Slf4j
@Builder
public class LLMCardinalityEstimator implements IEstimator {
    @Builder.Default
    private final EEstimatorPrompt estimatorPrompt = EEstimatorPrompt.JSON_CARDINALITY_ESTIMATOR;
    @Builder.Default
    private final EEstimatorPrompt conditionalEstimatorPrompt = EEstimatorPrompt.JSON_CONDITIONAL_CARDINALITY_ESTIMATOR;
    // TODO: Models should not be configurable from the builder (even in the query executors)!
    @Builder.Default
    private final IModel model = new OllamaModel(OLLAMA_MODEL);

    @Override
    public double estimate(ITable table) {
        String prompt = estimatorPrompt.generate(table);
        log.debug("estimate prompt is: {}", prompt);
        String response = model.text(prompt);
        log.debug("Cardinality response is: {}", response);
        Map<String, Object> json = fromJsonToMap(response);
        if (!json.containsKey("result")) {
            log.warn("Unable to estimate value for table: {}", table.getName());
        }
        return Double.parseDouble(json.getOrDefault("result", -1.0).toString());
    }

    @Override
    public double estimateWithExpression(ITable table, Expression expression) {
        String prompt = conditionalEstimatorPrompt.generate(table, expression);
        log.debug("estimateWithExpression prompt is: {}", prompt);
        String response = model.text(prompt);
        log.debug("Conditional cardinality response is: {}", response);
        Map<String, Object> json = fromJsonToMap(response);
        if (!json.containsKey("result")) {
            log.warn("Unable to estimate value for table: {}", table.getName());
        }
        return Double.parseDouble(json.getOrDefault("result", -1.0).toString());
    }
}
