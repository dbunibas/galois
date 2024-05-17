package galois.llm.query.ollama;

import galois.llm.models.IModel;
import galois.llm.models.OllamaModel;
import galois.llm.query.IQueryExecutor;
import galois.prompt.ETablePrompts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.SpeedyConstants;
import speedy.model.database.*;

import java.util.Arrays;
import java.util.List;

import static galois.llm.query.QueryUtils.createNewTupleWithMockOID;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaLlama3TableQueryExecutor implements IQueryExecutor {
    @Builder.Default
    private final IModel model = new OllamaModel("llama3");
    @Builder.Default
    private final ETablePrompts tablePrompt = ETablePrompts.TABLE_PROMPT;

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ITable table = database.getTable(tableAlias.getTableName());
        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid"))
                .toList();

        String prompt = tablePrompt.generate(table, attributes);
        log.debug("Table prompt is: {}", prompt);
        String response = model.text(prompt);
        log.debug("Table response is: {}", response);

        return Arrays.stream(response.split("\n"))
                .skip(2)
                .map(row -> toTuple(row, tableAlias, attributes))
                .toList();
    }

    private Tuple toTuple(String answer, TableAlias tableAlias, List<Attribute> attributes) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);

        List<String> cells = Arrays.stream(answer.trim().split("\\|"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();

        if (cells.size() != attributes.size()) {
            log.warn("Cells size ({}) is different from attributes size ({})", cells.size(), attributes.size());
        }

        for (int i = 0; i < attributes.size(); i++) {
            IValue cellValue = cells.size() > i ?
                    new ConstantValue(cells.get(i)) :
                    new NullValue(SpeedyConstants.NULL_VALUE);
            Attribute attribute = attributes.get(i);
            Cell currentCell = new Cell(
                    tuple.getOid(),
                    new AttributeRef(tableAlias, attribute.getName()),
                    cellValue
            );
            tuple.addCell(currentCell);
        }

        return tuple;
    }
}
