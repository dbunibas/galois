package floq.prompt;

import floq.prompt.parser.attributes.CommaAttributesParser;
import floq.prompt.parser.IAttributesResponseParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import engine.model.database.Attribute;
import engine.model.database.ITable;

import java.util.Collection;

import static floq.llm.query.utils.QueryUtils.getAttributesAsString;

@Getter
@AllArgsConstructor
public enum EAttributesPrompts {
    ATTRIBUTES_PROMPT("List the ${attributes} of the ${table} ${key}.\nJust report the values in a row without any additional comments.", CommaAttributesParser::parse),
    ;

    private final String template;
    private final IAttributesResponseParser parser;

    public String generate(ITable table, String key, Collection<Attribute> attributes) {
        String attributesString = getAttributesAsString(attributes);
        return template
                .replaceAll("\\$\\{attributes\\}", attributesString)
                .replaceAll("\\$\\{table\\}", table.getName())
                .replaceAll("\\$\\{key\\}", key);
    }
}
