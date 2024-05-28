package galois.prompt;

import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.Collection;

import static galois.llm.query.utils.QueryUtils.getAttributesAsString;

public enum ETablePrompts {
    TABLE_PROMPT("Given the following query, populate the table with actual values.\nquery: select ${attributes} from ${table}.\n${query}"),
    NATURAL_LANGUAGE_PROMPT("${query}"),
    ;

    private final String template;

    ETablePrompts(String template) {
        this.template = template;
    }

    public String generate(ITable table, Collection<Attribute> attributes, String query) {
        String attributesString = getAttributesAsString(attributes);
        return template
                .replaceAll("\\$\\{attributes\\}", attributesString)
                .replaceAll("\\$\\{table\\}", table.getName())
                .replaceAll("\\$\\{query\\}", query);
    }
}
