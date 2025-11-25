package galois.test.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class ExperimentVariant {
    String queryId;
    String querySQL;
    String queryUDF;
    String prompt;
    @Builder.Default
    List<String> optimizers = List.of();
}
