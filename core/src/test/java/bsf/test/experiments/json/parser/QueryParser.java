package bsf.test.experiments.json.parser;

import bsf.test.experiments.Query;
import bsf.test.experiments.json.QueryJSON;
import queryexecutor.model.database.IDatabase;
import queryexecutor.persistence.relational.AccessConfiguration;


public class QueryParser {
    public static Query parseJSON(QueryJSON json) {
        AccessConfiguration accessConfiguration = AccessConfigurationParser.getAccessConfiguration(json.getSchema());
        IDatabase database = LLMDatabaseParser.parseDatabase(json.getSchema());
//        List<Tuple> results = TestUtils.loadTuplesFromCSV(json.getResults(), true);
        //return new Query(json.getSql(), accessConfiguration, database, results, json.getResults());
        return new Query(json.getSql(), accessConfiguration, database, json.getResults());
    }
}
