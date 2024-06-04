package galois.test.experiments.json.config;

import lombok.Data;

@Data
public class ScanConfigurationJSON {
    private String queryExecutor;
    private String firstPrompt;
    private String iterativePrompt;
    private int maxIterations;
    private String attributesPrompt;
    private String prompt;
    private String sql;
}
