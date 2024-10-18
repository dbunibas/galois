package floq.prompt;

import floq.prompt.parser.IAttributesResponseParser;
import floq.prompt.parser.IEntitiesResponseParser;
import floq.prompt.parser.IKeyResponseParser;
import floq.prompt.parser.attributes.CommaAttributesParser;
import floq.prompt.parser.attributes.PipeAttributesParser;
import floq.prompt.parser.entities.JSONEntitiesParser;
import floq.prompt.parser.entities.MistralTableEntitiesParser;
import floq.prompt.parser.key.CommaKeyParser;
import floq.prompt.parser.key.PipeKeyParser;
import floq.utils.Mapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import engine.model.database.Attribute;
import engine.model.database.ITable;
import engine.model.database.Key;
import engine.model.expressions.Expression;

import java.util.Collection;
import java.util.List;

import static floq.llm.query.utils.QueryUtils.*;

@Getter
@AllArgsConstructor
public enum EPrompts {
    // Keys
//    LIST_KEY_JSON("List the ${key} of some ${table}s.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", Mapper::fromJsonListToList, null, null),
    LIST_KEY_JSON("List the ${key} of ${table}.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", Mapper::fromJsonListToListAndRemoveDuplicates, null, JSONEntitiesParser::parseAndRemoveDuplicates),
//    LIST_KEY_JSON_CONDITION("List the ${key} of some ${table}s where ${condition}.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", Mapper::fromJsonListToList, null, null),
//    LIST_KEY_JSON_CONDITION("List the ${key} of ${table}s where ${condition}.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", Mapper::fromJsonListToList, null, JSONEntitiesParser::parse),
    LIST_KEY_JSON_CONDITION("List the ${key} of ${table} where the following condition holds: ${condition}.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", Mapper::fromJsonListToListAndRemoveDuplicates, null, JSONEntitiesParser::parseAndRemoveDuplicates),

    LIST_KEY_PIPE("List the ${key} of some ${table}. Just report the values in a row separated by | without any comments.", PipeKeyParser::parse, null, null),
    LIST_KEY_PIPE_CONDITION("List the ${key} of some ${table}s where ${condition}. Just report the values in a row separated by | without any comments.", PipeKeyParser::parse, null, null),

    LIST_KEY_COMMA("List the ${key} of some ${table}. Just report the values in a row separated by comma without any comments.", CommaKeyParser::parse, null, null),
    LIST_KEY_COMMA_CONDITION("List the ${key} of some ${table}s where ${condition}. Just report the values in a row separated by comma without any comments.", CommaKeyParser::parse, null, null),

    // Attributes
    ATTRIBUTES_PIPE("List the ${attributes} of the ${table} ${key}.\nJust report the values in a row separated by | without any additional comments.", null, PipeAttributesParser::parse, null),
    ATTRIBUTES_COMMA("List the ${attributes} of the ${table} ${key}.\nJust report the values in a row separated by comma without any additional comments.", null, CommaAttributesParser::parse, null),
//    ATTRIBUTES_JSON("List the ${attributes} of the ${table} ${key}.\nRespond with JSON only.\nUse the following JSON schema, but ignore the title:\n${jsonSchema}", null, (String response, List<Attribute> attributes) -> Mapper.fromJsonToMap(response), null),
    ATTRIBUTES_JSON("List the ${attributes} of the ${table} ${key}.\nRespond with JSON only. Return all numerical attributes in valid numerical format. Don't use the comma in the numerical values. Use the dot for floating numbers.\nUse the following JSON schema, but ignore the title:\n${jsonSchema}", null, (String response, List<Attribute> attributes) -> Mapper.fromJsonToMap(response), null),
    // TODO: Add attributes prompt with auto-generated example from the table attributes

    // Entities
    FROM_TABLE_JSON("Given the following query, populate the table with actual values.\nquery: select ${attributes} from ${table}.\nRespond with JSON only. Don't add any comment.\nUse the following JSON schema:\n${jsonSchema}", null, null, JSONEntitiesParser::parseAndRemoveDuplicates),
    FROM_TABLE_JSON_CONDITION("Given the following query, populate the table with actual values.\nquery: select ${attributes} from ${table} where ${condition}.\nRespond with JSON only. Don't add any comment.\nUse the following JSON schema:\n${jsonSchema}", null, null, JSONEntitiesParser::parseAndRemoveDuplicates),

    FROM_TABLE_MISTRAL("Given the following query, populate the table with actual values.\nquery: select ${attributes} from ${table}.\nInclude all the values that you know. Just report the table without any comment.", null, null, MistralTableEntitiesParser::parse),

    FROM_SQL_JSON("List the result of the SQL query:\n${sql}.\nRespond with JSON only.\nUse the following JSON schema:\n${jsonSchema}", null, null, JSONEntitiesParser::parse),

    // Natural Language
    NATURAL_LANGUAGE_JSON("${prompt}\nRespond with JSON only. Don't add any comment.\nUse the following JSON schema:\n${jsonSchema}", null, null, JSONEntitiesParser::parse),

    // Iterative
    LIST_DIFFERENT_VALUES("List different values.", null, null, null),
//    LIST_DIFFERENT_VALUES_JSON("List different values. Respond with JSON only.", null, null, null),
    LIST_DIFFERENT_VALUES_JSON("List more values if there are more, otherwise return an empty JSON. Respond with JSON only.", null, null, null),
//    LIST_MORE_NO_REPEAT("List more values. Don't repeat the previous values.", null, null, null),
//    LIST_MORE_NO_REPEAT("List more values if there are more, otherwise return an empty response. Don't repeat the previous values.", null, null, null),
    LIST_MORE_NO_REPEAT("List more unique values if there are more, otherwise return an empty response. Don't repeat the previous values.", null, null, null),
    
    // JSON Error correction
    ERROR_JSON_FORMAT("Respond in an appropriate JSON format.", null, null, null),
    ERROR_JSON_NUMBER_FORMAT("Respond in an appropriate JSON format for a numerical value. Do not use the thousands separator.", null, null, null),
    ;

    private final String template;
    private final IKeyResponseParser keyParser;
    private final IAttributesResponseParser attributesParser;
    private final IEntitiesResponseParser entitiesParser;

    public String generate() {
        return template;
    }

    public String generateUsingNL(String prompt, String jsonSchema) {
        return generate(null, null, null, null, prompt, null, jsonSchema);
    }

    public String generateUsingSQL(String sql, String jsonSchema) {
        return generate(null, null, null, null, null, sql, jsonSchema);
    }

    public String generate(ITable table, Key primaryKey, Expression condition, String jsonSchema) {
        return generate(table.getName(), getKeyAsString(primaryKey), null, getExpressionAsString(condition), null, null, jsonSchema);
    }

    public String generate(ITable table, String key, Collection<Attribute> attributes, String jsonSchema) {
        return generate(table.getName(), key, getAttributesAsString(attributes), null, null, null, jsonSchema);
    }

    public String generate(ITable table, Collection<Attribute> attributes, String jsonSchema) {
        return generate(table.getName(), null, getAttributesAsString(attributes), null, null, null, jsonSchema);
    }

    public String generate(ITable table, Collection<Attribute> attributes, Expression condition, String jsonSchema) {
        return generate(table.getName(), null, getAttributesAsString(attributes), getExpressionAsString(condition), null, null, jsonSchema);
    }

    private String generate(String tableName, String primaryKey, String attributes, String condition, String prompt, String sql, String jsonSchema) {
        String result = replaceAll(template, "\\$\\{table\\}", tableName);
        result = replaceAll(result, "\\$\\{key\\}", primaryKey);
        result = replaceAll(result, "\\$\\{attributes\\}", attributes);
        result = replaceAll(result, "\\$\\{condition\\}", condition);
        result = replaceAll(result, "\\$\\{prompt\\}", prompt);
        result = replaceAll(result, "\\$\\{sql\\}", sql);
        result = replaceAll(result, "\\$\\{jsonSchema\\}", jsonSchema);
        return result;
    }

    private String replaceAll(String template, String pattern, String replacement) {
        return replacement == null ? template : template.replaceAll(pattern, replacement);
    }
}
