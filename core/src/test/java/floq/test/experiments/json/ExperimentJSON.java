package floq.test.experiments.json;

import floq.test.experiments.json.config.OperatorsConfigurationJSON;
import lombok.Data;

import java.util.List;

@Data
public class ExperimentJSON {
    private String name;
    private String dbms;
    private OperatorsConfigurationJSON operatorsConfig;
    private List<String> metrics;
    private List<String> optimizers;
    private QueryJSON query;
    private String queryExecutor;
}
