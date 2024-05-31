package galois.prompt;

import galois.prompt.parser.IAttributesResponseParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import speedy.model.database.ITable;
import speedy.model.expressions.Expression;

@Getter
@AllArgsConstructor
public enum EEstimatorPrompt {
    JSON_CARDINALITY_ESTIMATOR("Estimate the number of ${table}s. Answer in JSON format with a result property. Do not provide any explanation. Do not use comments in JSON. Do not justify your answer.", null),
    JSON_CONDITIONAL_CARDINALITY_ESTIMATOR("Estimate the number of ${table}s where ${expression}. Answer in JSON format with a result property. Do not provide any explanation. Do not use comments in JSON. Do not justify your answer.", null),
    ;

    private final String template;
    private final IAttributesResponseParser parser;

    public String generate(ITable table) {
        return template.replaceAll("\\$\\{table\\}", table.getName());
    }

    public String generate(ITable table, Expression expression) {
        return template
                .replaceAll("\\$\\{table\\}", table.getName())
                .replaceAll("\\$\\{expression\\}", expression.getExpressionString());
    }
}
