package galois.llm.query.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import galois.llm.query.IQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static galois.llm.query.QueryUtils.createNewTupleWithMockOID;

@Slf4j
public class OllamaLLama3KeyValuesQueryExecutor implements IQueryExecutor {
    private final ChatLanguageModel model;

    private final int maxKeyIterations = 5;
    private int currentKeyIteration = 0;

    public OllamaLLama3KeyValuesQueryExecutor() {
        this.model = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName("llama3")
                .temperature(0.0)
                .build();
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ITable table = database.getTable(tableAlias.getTableName());
        Key primaryKey = database.getPrimaryKey(table.getName());
        List<String> primaryKeyAttributes = primaryKey.getAttributes().stream()
                .map(AttributeRef::getName)
                .toList();

        // TODO: Add http retry mechanism?
        Set<String> keyValues = generateKeyValues(table, primaryKey);
        log.debug("keyValues: {}", keyValues);

        return keyValues.stream()
                .map(k -> generateTupleFromKey(k, table, tableAlias, primaryKeyAttributes))
                .toList();
    }

    private Set<String> generateKeyValues(ITable table, Key primaryKey) {
        // TODO: Check composite primary key
        String key = primaryKey.getAttributes().stream()
                .map(AttributeRef::getName)
                .collect(Collectors.joining(" and "));

        Set<String> keys = Set.of();
        Set<String> lastKeys = Set.of();

        while (currentKeyIteration < maxKeyIterations) {
            String prompt = currentKeyIteration == 0 ?
                    getKeyPrompt(table, key) :
                    getIterativeKeyPrompt(table, key, lastKeys);

            String response = model.generate(prompt);
            lastKeys = Arrays.stream(response.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
            keys = Stream.concat(keys.stream(), lastKeys.stream()).collect(Collectors.toUnmodifiableSet());

            currentKeyIteration++;
        }

        return keys;
    }

    private Tuple generateTupleFromKey(String key, ITable table, TableAlias tableAlias, List<String> primaryKeyAttributes) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(key, tableAlias, tuple, primaryKeyAttributes);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                .toList();
        addValueFromAttributes(table, tableAlias, attributes, tuple, key);

        return tuple;
    }

    private void addCellForPrimaryKey(String key, TableAlias tableAlias, Tuple tuple, List<String> primaryKeyAttributes) {
        // TODO: Handle composite key
        Cell keyCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, primaryKeyAttributes.get(0)),
                new ConstantValue(key)
        );
        tuple.addCell(keyCell);
    }

    private void addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key) {
        for (Attribute attribute : attributes) {
            String prompt = getAttributePrompt(table, attribute, key);
            String response = model.generate(prompt);
            log.debug("attribute response is: {}", response);

            IValue cellValue = new ConstantValue(response);
            Cell currentCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attribute.getName()),
                    cellValue
            );
            tuple.addCell(currentCell);
        }
    }

    private String getKeyPrompt(ITable table, String key) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[INST]\n");
        prompt.append("List the ").append(key);
        prompt.append(" of some ").append(table.getName()).append("s. ");
        prompt.append("Just report the values in a row separated by commas without any comments.");
        prompt.append("\n[/INST]");

        log.debug("Keys prompt is: {}", prompt);

        return prompt.toString();
    }

    private String getIterativeKeyPrompt(ITable table, String key, Collection<String> values) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[INST]\n");
//        prompt.append("Given the values: ").append(String.join(", ", values)).append("\n");
        prompt.append("Exclude the values: ").append(String.join(", ", values)).append(".\n");
//        prompt.append("Avoid repeating the values: ").append(String.join(", ", values)).append("\n");
        prompt.append("List the ").append(key);
        prompt.append(" of some other ").append(table.getName()).append("s. ");
        prompt.append("Just report the values in a row separated by commas without any comments.");
        prompt.append("\n[/INST]");

        log.debug("Iterative key prompt is: {}", prompt);

        return prompt.toString();
    }

    private String getAttributePrompt(ITable table, Attribute attribute, String key) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[INST]\n");
        prompt.append("List the ").append(attribute.getName());
        prompt.append(" of the ").append(table.getName()).append(" ").append(key).append(".\n");
        prompt.append("Just report the value in a row without any additional comments.");
        prompt.append("\n[/INST]");

        log.debug("Attribute prompt is: {}", prompt);

        return prompt.toString();
    }
}
