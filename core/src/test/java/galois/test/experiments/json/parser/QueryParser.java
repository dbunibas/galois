package galois.test.experiments.json.parser;

import galois.test.experiments.Query;
import galois.test.experiments.json.QueryJSON;
import galois.test.utils.TestUtils;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

public class QueryParser {
    public static Query parseJSON(QueryJSON json) {
        AccessConfiguration accessConfiguration = AccessConfigurationParser.getAccessConfiguration(json.getSchema());
        IDatabase database = LLMDatabaseParser.parseDatabase(json.getSchema());
        List<Tuple> results = TestUtils.loadTuplesFromCSV(json.getResults(), true);
        return new Query(json.getSql(), accessConfiguration, database, results, json.getResults());
    }
}
