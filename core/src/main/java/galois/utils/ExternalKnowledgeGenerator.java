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

    private static final String EXTERNAL_KNOWLEDGE_MULTIPLE_TABLES_PROMPT = """
            Given the following tables:
            
            ${tablesContent}
            
            Respond to the following user query using only the information available in the table.
            
            User query:""";

    private static final String SINGLE_TABLE_PROMPT = """
            Name ${tableName}
            --
            ${tableContent}
            """;

    private static final ExternalKnowledgeGenerator G = new ExternalKnowledgeGenerator();

    @Getter
    @Setter
    private boolean generate = false;

    @Setter
    private ITable table = null;
    @Setter
    private List<ITable> tables = null;

    private ExternalKnowledgeGenerator() {
    }

    public static ExternalKnowledgeGenerator getInstance() {
        return G;
    }

    public String generateExternalKnowledge() {
        if (generate && table == null && tables == null) {
            throw new IllegalArgumentException("Cannot generate external knowledge without a table!");
        }

        if (generate && tables != null && tables.size() > 1) {
            String prompt = generateExternalKnowledgeFromMultipleTables();
            log.trace("prompt:\n{} ... (truncated to first 1000 chars)", prompt.substring(0, Math.min(prompt.length(), 1000)));
            return prompt;
        }

        assert tables == null || tables.getFirst().getName().equals(table.getName());
        String tableName = table.getName();
        String tableContent = generateTableContent(table);
        String prompt = formatPrompt(EXTERNAL_KNOWLEDGE_PROMPT, tableName, tableContent);
        log.trace("prompt:\n{} ... (truncated to first 1000 chars)", prompt.substring(0, Math.min(prompt.length(), 1000)));

        return prompt;
    }

    public String generateExternalKnowledgeFromMultipleTables() {
        StringBuilder tablesContentBuilder = new StringBuilder();

        for (ITable current : tables) {
            String tableName = current.getName();
            String tableContent = generateTableContent(current);
            String prompt = formatPrompt(SINGLE_TABLE_PROMPT, tableName, tableContent);
            tablesContentBuilder.append(prompt).append("\n");
        }

        return EXTERNAL_KNOWLEDGE_MULTIPLE_TABLES_PROMPT.replace("${tablesContent}", tablesContentBuilder);
    }

    private String generateTableContent(ITable table) {
        List<String> attributes = table.getAttributes()
                .stream()
                .map(Attribute::getName)
                .filter(s -> !s.equals("oid"))
                .toList();
        String header = String.join(",", attributes);

        StringBuilder bodyBuilder = new StringBuilder();
        ITupleIterator iterator = getTableTupleIterator(table);
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

    private ITupleIterator getTableTupleIterator(ITable table) {
        if (!(table instanceof DBMSTable dbmsTable)) {
            return table.getTupleIterator();
        }

        AccessConfiguration configuration = dbmsTable.getAccessConfiguration().clone();
        configuration.setSchemaName("public");

        return new DBMSDB(configuration).getTable(dbmsTable.getName()).getTupleIterator();
    }

    private String formatPrompt(String prompt, String tableName, String tableContent) {
        return prompt
                .replace("${tableName}", tableName)
                .replace("${tableContent}", tableContent);
    }
}
