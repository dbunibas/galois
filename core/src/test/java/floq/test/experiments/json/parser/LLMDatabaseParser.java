package floq.test.experiments.json.parser;

import floq.llm.database.LLMDB;
import floq.test.experiments.json.AttributeJSON;
import floq.test.experiments.json.SchemaJSON;
import floq.test.experiments.json.TableJSON;
import lombok.extern.slf4j.Slf4j;
import engine.model.algebra.operators.ICreateTable;
import engine.model.algebra.operators.sql.SQLCreateTable;
import engine.model.database.Attribute;
import engine.model.database.IDatabase;
import engine.model.database.dbms.DBMSDB;
import engine.persistence.relational.AccessConfiguration;
import engine.persistence.relational.QueryManager;
import engine.utility.DBMSUtility;

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
                .map(json -> new Attribute(table.getTableName(), json.getName(), json.getEngineAttributeType(), json.getNullable()))
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
