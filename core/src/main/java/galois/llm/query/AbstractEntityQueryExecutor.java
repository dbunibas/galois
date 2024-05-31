package galois.llm.query;

import dev.langchain4j.chain.ConversationalChain;
import galois.prompt.EPrompts;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.*;

@Slf4j
public abstract class AbstractEntityQueryExecutor implements IQueryExecutor {
    abstract protected ConversationalChain getConversationalChain();

    abstract protected EPrompts getFirstPrompt();

    abstract protected EPrompts getIterativePrompt();

    abstract protected int getMaxIterations();

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = getConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        List<Attribute> attributes = getCleanAttributes(table);
        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributes);

        List<Tuple> tuples = new ArrayList<>();

        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0 ?
                    generateFirstPrompt(table, attributes, jsonSchema) :
                    generateIterativePrompt(table, attributes, jsonSchema);
            log.debug("Prompt is: {}", userMessage);

            String response = chain.execute(userMessage);
            log.debug("Response is: {}", response);

            List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response);
            log.debug("Parsed response is: {}", parsedResponse);

            for (Map<String, Object> map : parsedResponse) {
                Tuple tuple = mapToTuple(map, tableAlias, attributes);
                // TODO: Handle possible duplicates
                tuples.add(tuple);
            }
        }

        return tuples;
    }

    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getFirstPrompt().generate(table, attributes, jsonSchema);
    }

    protected String generateIterativePrompt(ITable table, List<Attribute> attributes, String jsonSchema) {
        return getIterativePrompt().generate(table, attributes, jsonSchema);
    }
}
