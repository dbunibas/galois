package galois.test.model;

import lombok.Value;

import java.util.List;

@Value
public class ExpVariant {
    private String queryNum;
    private String querySql;
    private String prompt;
    private List<String> optimizers;
}
