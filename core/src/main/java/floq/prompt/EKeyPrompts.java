package floq.prompt;

import floq.prompt.parser.key.CommaKeyParser;
import floq.prompt.parser.IKeyResponseParser;
import floq.prompt.parser.key.PipeKeyParser;
import lombok.Getter;
import engine.model.database.ITable;
import engine.model.database.Key;
import engine.model.expressions.Expression;

import static floq.llm.query.utils.QueryUtils.getKeyAsString;

@Getter
public enum EKeyPrompts {

    KEY_PROMPT("List the ${key} of some ${table}s. Just report the values in a row separated by | without any comments.", PipeKeyParser::parse),
    KEY_PROMPT_COMMA("List the ${key} of some ${table}s. Just report the values in a row separated by comma without any comments.", CommaKeyParser::parse),

    CONDITIONAL_KEY_PROMPT("Who are the ${table}s where ${expression}? Report the results in a row and separate the values by |. Do not add any comments.", PipeKeyParser::parse),
    ;

    private final String template;
    private final IKeyResponseParser parser;

    EKeyPrompts(String template, IKeyResponseParser parser) {
        this.template = template;
        this.parser = parser;
    }

    public String generate(ITable table, Key primaryKey) {
        String key = getKeyAsString(primaryKey);
        return template
                .replaceAll("\\$\\{key\\}", key)
                .replaceAll("\\$\\{table\\}", table.getName());
    }

    public String generate(ITable table, Key primaryKey, Expression expression) {
        if (expression == null) return generate(table, primaryKey);
        String key = getKeyAsString(primaryKey);
        return template
                .replaceAll("\\$\\{key\\}", key)
                .replaceAll("\\$\\{table\\}", table.getName())
                .replaceAll("\\$\\{expression\\}", expression.getExpressionString());
    }
}
