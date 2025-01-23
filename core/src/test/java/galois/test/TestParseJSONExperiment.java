package galois.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.database.LLMDB;
import galois.llm.query.ollama.mistral.OllamaMistralTableQueryExecutor;
import galois.test.experiments.Experiment;
import galois.test.experiments.Query;
import galois.test.experiments.json.*;
import galois.test.experiments.json.config.OperatorsConfigurationJSON;
import galois.test.experiments.json.config.ScanConfigurationJSON;
import galois.test.experiments.json.parser.ExperimentParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import speedy.model.database.IDatabase;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TestParseJSONExperiment {

    @Test
    public void testParseToExperimentJSON() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL jsonResource = TestParseJSONExperiment.class.getResource("/continents/continents-llama3-table-experiment.json");
        ExperimentJSON experimentJSON = mapper.readValue(jsonResource, ExperimentJSON.class);

        Assertions.assertEquals("Dummy Experiment", experimentJSON.getName());
        Assertions.assertEquals("postgres", experimentJSON.getDbms());

        Assertions.assertEquals(1, experimentJSON.getMetrics().size());
        Assertions.assertEquals("DummyMetric", experimentJSON.getMetrics().get(0));

        OperatorsConfigurationJSON operatorsConfigurationJSON = experimentJSON.getOperatorsConfig();
        Assertions.assertNotNull(operatorsConfigurationJSON);
        ScanConfigurationJSON scanConfigurationJSON = operatorsConfigurationJSON.getScan();
        Assertions.assertNotNull(scanConfigurationJSON);
        Assertions.assertEquals(OllamaMistralTableQueryExecutor.class.getSimpleName(), scanConfigurationJSON.getQueryExecutor());

        QueryJSON queryJSON = experimentJSON.getQuery();
        Assertions.assertEquals("select * from target.continent c order by c.name", queryJSON.getSql());
        Assertions.assertEquals("/continents/continent", queryJSON.getResults());

        SchemaJSON schemaJSON = queryJSON.getSchema();
        Assertions.assertNotNull(schemaJSON);
        Assertions.assertEquals(1, schemaJSON.getTables().size());

        TableJSON tableJSON = schemaJSON.getTables().get(0);
        Assertions.assertEquals("continent", tableJSON.getTableName());

        List<AttributeJSON> attributes = tableJSON.getAttributes();
        Assertions.assertEquals(3, attributes.size());
        attributes.forEach(this::testDummyExperimentsAttribute);
    }

    private void testDummyExperimentsAttribute(AttributeJSON attributeJSON) {
        switch (attributeJSON.getName()) {
            case "name":
                Assertions.assertEquals("STRING", attributeJSON.getType());
                Assertions.assertFalse(attributeJSON.getNullable());
                Assertions.assertTrue(attributeJSON.getKey());
                return;
            case "area", "population":
                Assertions.assertEquals("STRING", attributeJSON.getType());
                Assertions.assertFalse(attributeJSON.getNullable());
                Assertions.assertFalse(attributeJSON.getKey());
                return;
            default:
                Assertions.fail(attributeJSON.getName());
        }
    }

    @Test
    public void testParseToExperiment() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL jsonResource = TestParseJSONExperiment.class.getResource("/continents/continents-llama3-table-experiment.json");
        ExperimentJSON experimentJSON = mapper.readValue(jsonResource, ExperimentJSON.class);
        Assertions.assertNotNull(experimentJSON);

        Experiment experiment = ExperimentParser.parseJSON(experimentJSON);
        Assertions.assertNotNull(experiment);
        Assertions.assertEquals(experimentJSON.getName(), experiment.getName());

        Assertions.assertEquals(experimentJSON.getMetrics().size(), experiment.getMetrics().size());
        for (int i = 0; i < experimentJSON.getMetrics().size(); i++) {
            Assertions.assertEquals(experimentJSON.getMetrics().get(i), experiment.getMetrics().get(i).getName());
        }

        OperatorsConfiguration operatorsConfiguration = experiment.getOperatorsConfiguration();
        Assertions.assertNotNull(operatorsConfiguration);
        ScanConfiguration scanConfiguration = operatorsConfiguration.getScan();
        Assertions.assertNotNull(scanConfiguration);
        Assertions.assertTrue(scanConfiguration.getQueryExecutor() instanceof OllamaMistralTableQueryExecutor);

        QueryJSON queryJSON = experimentJSON.getQuery();
        Query query = experiment.getQuery();
        Assertions.assertNotNull(query);
        Assertions.assertEquals(queryJSON.getSql(), query.getSql());

        IDatabase database = query.getDatabase();
        Assertions.assertNotNull(database);
        Assertions.assertEquals(1, database.getTableNames().size());
        Assertions.assertEquals("continent", database.getFirstTable().getName());

//        Assertions.assertEquals(6, query.getResults().size());
    }

    @Test
    public void testLoadAndParseToExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/dummy-experiment.getjson");
        Assertions.assertNotNull(experiment);
        Assertions.assertEquals("Dummy Experiment", experiment.getName());

        Assertions.assertNotNull(experiment.getMetrics());
        Assertions.assertEquals(1, experiment.getMetrics().size());
        Assertions.assertEquals("DummyMetric", experiment.getMetrics().get(0).getName());

        Assertions.assertNotNull(experiment.getQuery());
        Assertions.assertTrue(experiment.getQuery().getDatabase() instanceof LLMDB);
//        Assertions.assertEquals(6, experiment.getQuery().getResults().size());
    }
}
