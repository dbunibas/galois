package galois.prompt;

import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.Collection;

import static galois.llm.query.QueryUtils.getAttributesAsString;

public enum ETablePrompts {
    TABLE_PROMPT("Given the following query, populate the table with actual values.\nquery: select ${attributes} from ${table}s.\nInclude all the values that you know. Just report the table without any comment.");

    private final String template;

    ETablePrompts(String template) {
        this.template = template;
    }

    public String generate(ITable table, Collection<Attribute> attributes) {
        String attributesString = getAttributesAsString(attributes);
        return template
                .replaceAll("\\$\\{attributes\\}", attributesString)
                .replaceAll("\\$\\{table\\}", table.getName());
    }
}
