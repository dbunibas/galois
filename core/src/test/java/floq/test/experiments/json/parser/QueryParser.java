package floq.test.experiments.json.parser;

import floq.test.experiments.Query;
import floq.test.experiments.json.QueryJSON;
import engine.model.database.IDatabase;
import engine.persistence.relational.AccessConfiguration;


public class QueryParser {
    public static Query parseJSON(QueryJSON json) {
        AccessConfiguration accessConfiguration = AccessConfigurationParser.getAccessConfiguration(json.getSchema());
        IDatabase database = LLMDatabaseParser.parseDatabase(json.getSchema());
//        List<Tuple> results = TestUtils.loadTuplesFromCSV(json.getResults(), true);
        //return new Query(json.getSql(), accessConfiguration, database, results, json.getResults());
        return new Query(json.getSql(), accessConfiguration, database, json.getResults());
    }
}
