package galois.prompt;

import galois.prompt.key.CommaKeyParser;
import galois.prompt.key.IKeyResponseParser;
import galois.prompt.key.PipeKeyParser;
import lombok.Getter;
import speedy.model.database.ITable;
import speedy.model.database.Key;

import static galois.llm.query.QueryUtils.getKeyAsString;

@Getter
public enum EKeyPrompts {

    KEY_PROMPT("List the ${key} of some ${table}s. Just report the values in a row separated by | without any comments.", PipeKeyParser::parse),
    KEY_PROMPT_COMMA("List the ${key} of some ${table}s. Just report the values in a row separated by comma without any comments.", CommaKeyParser::parse),
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
}
