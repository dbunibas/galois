package galois.llm.query.outlines;

import galois.llm.models.IModel;
import galois.llm.models.OutlinesModel;
import galois.llm.query.IQueryExecutor;
import galois.prompt.EAttributesPrompts;
import galois.prompt.EIterativeKeyPrompts;
import galois.prompt.EKeyPrompts;
import galois.prompt.parser.IKeyResponseParser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.SpeedyConstants;
import speedy.model.database.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static galois.llm.query.utils.QueryUtils.createNewTupleWithMockOID;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaFromAttribute;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutlinesKeyValueQueryExecutor implements IQueryExecutor {
    // private static final String MODEL_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final String MODEL_NAME = "Meta-Llama-3-8B-Instruct.Q4_K_M.gguf";

    @Builder.Default
    private final IModel model = new OutlinesModel(MODEL_NAME);
    @Builder.Default
    private final EKeyPrompts keyPrompt = EKeyPrompts.KEY_PROMPT;
    @Builder.Default
    private final EIterativeKeyPrompts iterativeKeyPrompt = EIterativeKeyPrompts.ITERATIVE_PROMPT;
    @Builder.Default
    private final EAttributesPrompts attributesPrompt = EAttributesPrompts.ATTRIBUTES_PROMPT;

    @Builder.Default
    private final int maxKeyIterations = 1;

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
        Set<String> keys = Set.of();
        Set<String> lastKeys = Set.of();
        int currentKeyIteration = 0;

        while (currentKeyIteration < maxKeyIterations) {
            String prompt = currentKeyIteration == 0 ?
                    keyPrompt.generate(table, primaryKey) :
                    iterativeKeyPrompt.generate(table, primaryKey, lastKeys);
            log.debug("Key prompt is: {}", prompt);
            IKeyResponseParser responseParser = currentKeyIteration == 0 ?
                    keyPrompt.getParser() :
                    iterativeKeyPrompt.getParser();

            String response = model.text(prompt);
            lastKeys = responseParser.parse(response).stream().collect(Collectors.toUnmodifiableSet());
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
        attributes.forEach(a -> addValueFromAttribute(table, tableAlias, a, tuple, key));

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

    private void addValueFromAttribute(ITable table, TableAlias tableAlias, Attribute attribute, Tuple tuple, String key) {
        String prompt = attributesPrompt.generate(table, key, List.of(attribute));
        log.debug("Attribute prompt is: {}", prompt);
        String schema = generateJsonSchemaFromAttribute(table, attribute);
//        TODO: Why json and not text / regex?
        Map<String, Object> jsonMap = model.json(prompt, schema);

        IValue cellValue = jsonMap.containsKey(attribute.getName()) ?
                new ConstantValue(jsonMap.get(attribute.getName())) :
                new NullValue(SpeedyConstants.NULL_VALUE);
        Cell currentCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, attribute.getName()),
                cellValue
        );

        tuple.addCell(currentCell);
    }
}
