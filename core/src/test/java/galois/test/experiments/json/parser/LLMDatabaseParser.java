package galois.test.experiments.json.parser;

import galois.llm.database.LLMDB;
import galois.test.experiments.json.AttributeJSON;
import galois.test.experiments.json.SchemaJSON;
import galois.test.experiments.json.TableJSON;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.operators.ICreateTable;
import speedy.model.algebra.operators.sql.SQLCreateTable;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;
import speedy.utility.DBMSUtility;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class LLMDatabaseParser {
    public static IDatabase parseDatabase(SchemaJSON schema) {
        AccessConfiguration accessConfiguration = AccessConfigurationParser.getAccessConfiguration(schema);
        initDB(accessConfiguration, schema.getTables());
        return new LLMDB(accessConfiguration);
    }

    private static void initDB(AccessConfiguration accessConfiguration, List<TableJSON> tables) {
        DBMSDB db = new DBMSDB(accessConfiguration);

        if (DBMSUtility.isDBExists(accessConfiguration) && DBMSUtility.isSchemaExists(accessConfiguration)) {
            DBMSUtility.removeSchema(accessConfiguration.getSchemaName(), accessConfiguration);
        }

        db.getInitDBConfiguration().setInitDBScript(createSchema(accessConfiguration.getSchemaName()));
        db.initDBMS();

        if (!db.getTableNames().isEmpty()) {
            log.error("Tables size is greater than 0: {}. Deleting the DB is currently unsupported!", db.getTableNames().size());
            throw new RuntimeException("Tables size is greater than 0: {}. Deleting the DB is currently unsupported!");
        }

        ICreateTable tableGenerator = new SQLCreateTable();
        tables.forEach(t -> tableGenerator.createTable(t.getTableName(), getTableAttributes(t), getKeysAttributes(t), db));

        Connection connection = QueryManager.getConnection(accessConfiguration);
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Cannot close non-null connection! {}", connection);
            }
        }
    }

    private static List<Attribute> getTableAttributes(TableJSON table) {
        return table.getAttributes().stream()
                .map(json -> new Attribute(table.getTableName(), json.getName(), json.getType(), json.getNullable()))
                .toList();
    }

    private static Set<String> getKeysAttributes(TableJSON table) {
        return table.getAttributes().stream()
                .filter(AttributeJSON::getKey)
                .map(AttributeJSON::getName)
                .collect(Collectors.toSet());
    }

    private static String createSchema(String schemaName) {
        StringBuilder sb = new StringBuilder();
        if (schemaName == null || schemaName.isEmpty()) return "";
        return sb.append("create schema ").append(schemaName).append(";").toString();
    }
}
