package galois.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.*;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTable;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

@Slf4j
public class ExternalKnowledgeGenerator {
    private static final String EXTERNAL_KNOWLEDGE_PROMPT = """
            Given the following table named ${tableName}:
            
            ${tableContent}
            
            Respond to the following user query using only the information available in the table.
            
            User query:""";

    private static final ExternalKnowledgeGenerator G = new ExternalKnowledgeGenerator();

    @Getter
    @Setter
    private boolean generate = false;

    @Setter
    private ITable table = null;

    private ExternalKnowledgeGenerator() {
    }

    public static ExternalKnowledgeGenerator getInstance() {
        return G;
    }

    public String generateExternalKnowledge() {
        if (generate && table == null) {
            throw new IllegalArgumentException("Cannot generate external knowledge without a table!");
        }

        String tableName = table.getName();
        String tableContent = generateTableContent();
        String prompt = formatPrompt(tableName, tableContent);
        log.trace("prompt:\n{} ... (truncated to first 1000 chars)", prompt.substring(0, 1000));

        return prompt;
    }

    private String generateTableContent() {
        List<String> attributes = table.getAttributes()
                .stream()
                .map(Attribute::getName)
                .filter(s -> !s.equals("oid"))
                .toList();
        String header = String.join(",", attributes);

        StringBuilder bodyBuilder = new StringBuilder();
        ITupleIterator iterator = getTableTupleIterator();
        while (iterator.hasNext()) {
            StringBuilder tupleBuilder = new StringBuilder();
            Tuple tuple = iterator.next();
            for (String attribute : attributes) {
                Cell cell = tuple.getCell(new AttributeRef(table.getName(), attribute));
                tupleBuilder.append(cell.getValue().getPrimitiveValue().toString()).append(",");
            }
            bodyBuilder.append(tupleBuilder).append("\n");
        }
        iterator.close();

        return header + "\n" + bodyBuilder;
    }

    private ITupleIterator getTableTupleIterator() {
        if (!(table instanceof DBMSTable dbmsTable)) {
            return table.getTupleIterator();
        }

        AccessConfiguration configuration = dbmsTable.getAccessConfiguration().clone();
        configuration.setSchemaName("public");

        return new DBMSDB(configuration).getTable(dbmsTable.getName()).getTupleIterator();
    }

    private String formatPrompt(String tableName, String tableContent) {
        return EXTERNAL_KNOWLEDGE_PROMPT
                .replace("${tableName}", tableName)
                .replace("${tableContent}", tableContent);
    }
}
