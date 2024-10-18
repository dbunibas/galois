package floq.prompt;

import floq.prompt.parser.key.CommaKeyParser;
import floq.prompt.parser.IKeyResponseParser;
import floq.prompt.parser.key.PipeKeyParser;
import lombok.Getter;
import engine.model.database.ITable;
import engine.model.database.Key;
import engine.model.expressions.Expression;

import java.util.Collection;

import static floq.llm.query.utils.QueryUtils.getKeyAsString;

@Getter
public enum EIterativeKeyPrompts {
    ITERATIVE_PROMPT(
            "Exclude the values: ${values}.\nList the ${key} of some other ${table}s. Just report the values in a row separated by | without any comments.",
            PipeKeyParser::parse
    ),
    ITERATIVE_PROMPT_COMMA(
            "Exclude the values: ${values}.\nList the ${key} of some other ${table}s. Just report the values in a row separated by comma without any comments.",
            CommaKeyParser::parse
    ),

    ITERATIVE_CONDITIONAL_PROMPT(
            "Exclude the values: ${values}.\nWho are the ${table}s where ${expression}? Report the results in a row and separate the values by |. Do not add any comments.",
            PipeKeyParser::parse
    ),
    ;

    private final String template;
    private final IKeyResponseParser parser;

    EIterativeKeyPrompts(String template, IKeyResponseParser parser) {
        this.template = template;
        this.parser = parser;
    }

    public String generate(ITable table, Key primaryKey, Collection<String> previousValues) {
        String key = getKeyAsString(primaryKey);
        return template
                .replaceAll("\\$\\{values\\}", String.join(", ", previousValues))
                .replaceAll("\\$\\{key\\}", key)
                .replaceAll("\\$\\{table\\}", table.getName());
    }

    public String generate(ITable table, Key primaryKey, Collection<String> previousValues, Expression expression) {
        if (expression == null) return generate(table, primaryKey, previousValues);
        String key = getKeyAsString(primaryKey);
        return template
                .replaceAll("\\$\\{values\\}", String.join(", ", previousValues))
                .replaceAll("\\$\\{key\\}", key)
                .replaceAll("\\$\\{table\\}", table.getName())
                .replaceAll("\\$\\{expression\\}", expression.getExpressionString());
    }
}
