package galois.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class ExpVariant {
    String queryNum;
    String querySql;
    String prompt;
    @Builder.Default
    List<String> optimizers = List.of();
}
