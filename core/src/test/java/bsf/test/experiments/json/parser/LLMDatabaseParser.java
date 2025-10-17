package bsf.test.experiments.json.parser;

import bsf.llm.database.LLMDB;
import bsf.test.experiments.json.AttributeJSON;
import bsf.test.experiments.json.SchemaJSON;
import bsf.test.experiments.json.TableJSON;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.algebra.operators.ICreateTable;
import queryexecutor.model.algebra.operators.sql.SQLCreateTable;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.utility.DBMSUtility;

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
                .map(json -> new Attribute(table.getTableName(), json.getName(), json.getQueryExecutorAttributeType(), json.getNullable()))
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
