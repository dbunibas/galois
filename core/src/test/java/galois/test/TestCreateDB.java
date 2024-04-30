package galois.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.operators.ICreateTable;
import speedy.model.algebra.operators.sql.SQLCreateTable;
import speedy.model.database.Attribute;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;
import java.util.Set;

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

        String actorTableName = "actor";
        List<Attribute> actorAttributes = List.of(
                new Attribute(actorTableName, "name", "string"),
                new Attribute(actorTableName, "gender", "string"),
                new Attribute(actorTableName, "birth_year", "integer")
        );
        Set<String> actorPrimaryKeys = Set.of("name");

        String filmTableName = "film";
        List<Attribute> filmAttributes = List.of(
                new Attribute(filmTableName, "title", "string"),
                new Attribute(filmTableName, "director", "string"),
                new Attribute(filmTableName, "release_year", "integer")
        );
        Set<String> filmPrimaryKeys = Set.of("title");

        String directorTableName = "film_director";
        List<Attribute> directorAttributes = List.of(
                new Attribute(directorTableName, "name", "string"),
                new Attribute(directorTableName, "gender", "string"),
                new Attribute(directorTableName, "birth_year", "integer")
        );
        Set<String> directorPrimaryKeys = Set.of("name");

        ICreateTable tableGenerator = new SQLCreateTable();
        tableGenerator.createTable(actorTableName, actorAttributes, actorPrimaryKeys, db);
        tableGenerator.createTable(filmTableName, filmAttributes, filmPrimaryKeys, db);
        tableGenerator.createTable(directorTableName, directorAttributes, directorPrimaryKeys, db);

        Assertions.assertEquals(db.getTableNames().size(), 3);
    }

    private String createSchema(String schemaName) {
        StringBuilder sb = new StringBuilder();
        if (schemaName == null || schemaName.isEmpty()) return "";
        return sb.append("create schema ").append(schemaName).append(";").toString();
    }
}
