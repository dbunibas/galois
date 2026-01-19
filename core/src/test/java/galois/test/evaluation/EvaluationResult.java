package galois.test.evaluation;

import galois.llm.query.LLMQueryStatManager;
import galois.test.experiments.metrics.IMetric;
import lombok.Getter;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationResult {
    // Experiment
    private final String experimentName;
    @Getter
    private final ExperimentVariant variant;
    private final long executionTime;
    // Tuples
    private final List<Tuple> expected;
    private final List<Tuple> results;
    // Metrics
    private final List<IMetric> metrics;
    @Getter
    private Map<String, Double> scoresMap;
    // LLM stats
    @Getter
    private final int llmRequest;
    @Getter
    private final double llmTokensInput;
    @Getter
    private final double llmTokensOutput;
    @Getter
    private final long timeMs;

    public EvaluationResult(
            String experimentName,
            ExperimentVariant variant,
            long startTime,
            List<Tuple> expected,
            List<Tuple> results,
            List<IMetric> metrics
    ) {
        this.experimentName = experimentName;
        this.variant = variant;
        this.executionTime = System.currentTimeMillis() - startTime;

        this.expected = expected;
        this.results = results;
        this.metrics = metrics;

        this.llmRequest = LLMQueryStatManager.getInstance().getBaseLLMRequest() > 0 ?
                LLMQueryStatManager.getInstance().getBaseLLMRequest() :
                LLMQueryStatManager.getInstance().getLLMRequest();
        this.llmTokensInput = LLMQueryStatManager.getInstance().getBaseLLMTokensInput() > 0 ?
                LLMQueryStatManager.getInstance().getBaseLLMTokensInput() :
                LLMQueryStatManager.getInstance().getLLMTokensInput();
        this.llmTokensOutput = LLMQueryStatManager.getInstance().getBaseLLMTokensOutput() > 0 ?
                LLMQueryStatManager.getInstance().getBaseLLMTokensOutput() :
                LLMQueryStatManager.getInstance().getLLMTokensOutput();
        this.timeMs = LLMQueryStatManager.getInstance().getBasetimeMs() > 0 ?
                LLMQueryStatManager.getInstance().getBasetimeMs() :
                LLMQueryStatManager.getInstance().getTimeMs();
    }

    public void computeScores(IDatabase database) {
        scoresMap = new HashMap<>();
        for (IMetric metric : metrics) {
            scoresMap.put(metric.getName(), metric.getScore(database, expected, results));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Experiment: ").append(experimentName).append("-").append(variant.getQueryId()).append("\n");
        sb.append("Total execution time (ms): ").append(executionTime).append("\n");
        sb.append("\n");
        sb.append("Expected:").append(expected).append("\n");
        sb.append("Results:").append(results).append("\n");
        sb.append("\n");
        sb.append("Metrics").append("\n");
        for (String metric : scoresMap.keySet()) {
            sb.append("- ").append(metric).append(": ").append(scoresMap.get(metric)).append("\n");
        }
        sb.append("\n");
        sb.append("Stats").append("\n");
        sb.append("- # requests: ").append(llmRequest).append("\n");
        sb.append("- # input tokens: ").append(llmTokensInput).append("\n");
        sb.append("- # output tokens: ").append(llmTokensOutput).append("\n");
        sb.append("- time (ms): ").append(timeMs).append("\n");
        return sb.toString();
    }
}
