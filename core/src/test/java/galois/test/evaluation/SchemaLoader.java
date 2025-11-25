package galois.test.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import galois.test.experiments.json.parser.ExperimentParser;

import java.io.IOException;
import java.net.URL;

public class SchemaLoader {
    public static SchemaDatabase loadSchemaInExperimentFolder(String experimentFolder) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String schemaFile = experimentFolder + "/schema.json";
        URL jsonResource = ExperimentParser.class.getResource(schemaFile);
        if (jsonResource == null) throw new IllegalArgumentException("Unable to load file " + schemaFile);

        return mapper.readValue(jsonResource, SchemaDatabase.class);
    }
}
