package galois.llm.query.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import galois.llm.query.IQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import speedy.SpeedyConstants;
import speedy.model.database.*;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class OllamaMistralTableQueryExecutor implements IQueryExecutor {

    private final ChatLanguageModel model;

    public OllamaMistralTableQueryExecutor() {
        this.model = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName("mistral")
                .temperature(0.0)
                .build();
    }

    @Override
    public List<Tuple> execute(String query, ITable table) {
        String response = model.generate(getInitialPrompt(table));
        return new ArrayList<>(
                Arrays.stream(response.split("\n"))
                        .skip(2)
                        .map(row -> toTuple(row, table))
                        .toList()
        );
    }

    private String getInitialPrompt(ITable table) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Given the following query, populate the table with actual values.").append("\n");
        prompt.append("query: select ");

        String attributes = table.getAttributes().stream()
                .map(Attribute::getName)
                .filter(name -> !name.equals("oid"))
                .collect(Collectors.joining(", "));
        prompt.append(attributes).append(" ");

        prompt.append("from ").append(table.getName()).append("s.").append("\n");

        prompt.append("Include all the values that you know. Just report the table without any comment.");
        return prompt.toString();
    }

    private Tuple toTuple(String answer, ITable table) {
        TupleOID mockOID = new TupleOID(IntegerOIDGenerator.getNextOID());
        Tuple tuple = new Tuple(mockOID);
        Cell oidCell = new Cell(
                mockOID,
                new AttributeRef(table.getName(), SpeedyConstants.OID),
                new ConstantValue(mockOID)
        );
        tuple.addCell(oidCell);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid"))
                .toList();

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
                    mockOID,
                    new AttributeRef(table.getName(), attribute.getName()),
                    cellValue
            );
            tuple.addCell(currentCell);
        }

        return tuple;
    }
}
