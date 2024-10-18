package floq.test.experiments.json.parser;

import floq.test.experiments.json.SchemaJSON;
import engine.persistence.relational.AccessConfiguration;

public class AccessConfigurationParser {
    // TODO: This should be generalized (different DB, schemaName, username, ecc...)
    public static AccessConfiguration getAccessConfiguration(SchemaJSON schema) {
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:llm_" + schema.getName();

        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schema.getSchema());
        accessConfiguration.setLogin(schema.getUsername());
        accessConfiguration.setPassword(schema.getPassword());

        return accessConfiguration;
    }
}
