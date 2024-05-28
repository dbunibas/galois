package galois.llm.query.ollama.mistral;

import dev.langchain4j.chain.ConversationalChain;
import galois.llm.query.ConversationalChainFactory;
import galois.llm.query.IQueryExecutor;
import galois.prompt.ETablePrompts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static galois.llm.query.utils.QueryUtils.*;
import static galois.utils.Mapper.fromJsonToListOfMaps;

@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaMistralNLQueryExecutor implements IQueryExecutor {

    @Builder.Default
    private final ETablePrompts firstPrompt = ETablePrompts.NATURAL_LANGUAGE_PROMPT;
    @Builder.Default
    private final ETablePrompts iterativePrompt = ETablePrompts.NATURAL_LANGUAGE_PROMPT;
    @Builder.Default
    private final int maxIterations = 5;

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ConversationalChain chain = ConversationalChainFactory.buildOllamaMistralConversationalChain();

        ITable table = database.getTable(tableAlias.getTableName());

        List<Attribute> attributes = getCleanAttributes(table);
        String schema = generateJsonSchemaFromAttributes(table, attributes);

        List<Tuple> tuples = new ArrayList<>();

        for (int i = 0; i < maxIterations; i++) {
            String userMessage = i == 0 ?
                    firstPrompt.generate(table, List.of(), "List the name, gender and birth year of male film directors.\nRespond with JSON only.\nUse the following JSON schema:\n" + schema) :
                    iterativePrompt.generate(table, List.of(), "List different values.");
            log.debug("Prompt is: {}", userMessage);

            String response = chain.execute(userMessage);
            log.debug("Response is: {}", response);

            List<Map<String, Object>> parsedResponse = fromJsonToListOfMaps(response);
            log.debug("Parsed response is: {}", parsedResponse);

            for (Map<String, Object> map : parsedResponse) {
                Tuple tuple = mapToTuple(map, tableAlias, attributes);
                // TODO: Handle possible duplicates
                tuples.add(tuple);
            }
        }

        return tuples;
    }

}
