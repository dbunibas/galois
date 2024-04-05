package galois.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.operators.ICreateTable;
import speedy.model.algebra.operators.sql.SQLCreateTable;
import speedy.model.database.Attribute;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

public class TestCreateDB {
    @Test
    public void testCreateDummyActorsDB() {
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:speedy_llm_actors";
        String schemaName = "target";
        String username = "pguser";
        String password = "pguser";

        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);

        DBMSDB db = new DBMSDB(accessConfiguration);
        Assertions.assertNotNull(db);
        db.getInitDBConfiguration().setInitDBScript(createSchema(schemaName));
        db.initDBMS();

        String tableName = "actor";
        List<Attribute> attributes = List.of(
                new Attribute(tableName, "name", "string"),
                new Attribute(tableName, "sex", "string")
        );

        ICreateTable tableGenerator = new SQLCreateTable();
        tableGenerator.createTable(tableName, attributes, db);

        Assertions.assertEquals(db.getTableNames().size(), 1);
    }

    private String createSchema(String schemaName) {
        StringBuilder sb = new StringBuilder();
        if (schemaName == null || schemaName.isEmpty()) return "";
        return sb.append("create schema ").append(schemaName).append(";").toString();
    }
}
