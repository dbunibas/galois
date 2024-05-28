package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import galois.prompt.EPrompts;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static galois.llm.query.utils.QueryUtils.createNewTupleWithMockOID;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaForKeys;

@Slf4j
public abstract class AbstractKeyBasedQueryExecutor implements IQueryExecutor {
    abstract protected ConversationalChain getConversationalChain();

    abstract protected EPrompts getFirstPrompt();

    abstract protected EPrompts getIterativePrompt();

    abstract protected int getMaxIterations();

    protected abstract Tuple addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key, ConversationalChain chain);

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        Key primaryKey = database.getPrimaryKey(table.getName());
        Set<String> keyValues = getKeyValues(table, primaryKey, chain);

        return keyValues.stream().map(k -> generateTupleFromKey(table, tableAlias, k, primaryKey, chain)).toList();
    }

    private Set<String> getKeyValues(ITable table, Key primaryKey, ConversationalChain chain) {
        Set<String> keys = new HashSet<>();
        String schema = generateJsonSchemaForKeys(table);

        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0 ?
                    getFirstPrompt().generate(table, primaryKey, schema) :
                    getIterativePrompt().generate(table, primaryKey, schema);
            log.debug("Key prompt is: {}", userMessage);

            String response = chain.execute(userMessage);
            log.debug("Response is: {}", response);

            List<String> currentKeys = getFirstPrompt().getKeyParser().parse(response);
            log.debug("Parsed keys are: {}", currentKeys);
            keys.addAll(currentKeys);
        }

        return keys;
    }

    private Tuple generateTupleFromKey(ITable table, TableAlias tableAlias, String keyValue, Key primaryKey, ConversationalChain chain) {
        List<String> primaryKeyAttributes = primaryKey.getAttributes().stream().map(AttributeRef::getName).toList();
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(tuple, tableAlias, keyValue, primaryKeyAttributes);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                .toList();

        return addValueFromAttributes(table, tableAlias, attributes, tuple, keyValue, chain);
    }

    private void addCellForPrimaryKey(Tuple tuple, TableAlias tableAlias, String key, List<String> primaryKeyAttributes) {
        // TODO: Handle composite key
        Cell keyCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, primaryKeyAttributes.get(0)),
                new ConstantValue(key)
        );
        tuple.addCell(keyCell);
    }
}
