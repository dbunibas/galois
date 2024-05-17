package galois.prompt;

import galois.prompt.key.CommaKeyParser;
import galois.prompt.key.IKeyResponseParser;
import galois.prompt.key.PipeKeyParser;
import lombok.Getter;
import speedy.model.database.ITable;
import speedy.model.database.Key;

import java.util.Collection;

import static galois.llm.query.QueryUtils.getKeyAsString;

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
}
