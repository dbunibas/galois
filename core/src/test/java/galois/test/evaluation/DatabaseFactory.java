package galois.test.evaluation;

import lombok.extern.slf4j.Slf4j;
import speedy.model.database.IDatabase;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.DAOMainMemoryDatabase;
import speedy.persistence.relational.AccessConfiguration;

@Slf4j
public class DatabaseFactory {
    public static IDatabase connectToPostgres(String dbName, String schema, String username, String password) {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver("org.postgresql.Driver");
        accessConfiguration.setUri("jdbc:postgresql:" + dbName);
        accessConfiguration.setSchemaName(schema);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
        return new DBMSDB(accessConfiguration);
    }

    public static IDatabase connectToMainMemoryCSV(String dir, Character separator, Character quotes, boolean hasHeader) {
        return new DAOMainMemoryDatabase().loadCSVDatabase(dir, separator, quotes, false, hasHeader);
    }
}
