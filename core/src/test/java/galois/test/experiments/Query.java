package galois.test.experiments;

import galois.test.experiments.json.QueryJSON;
import lombok.Data;
import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import java.util.List;

@Data
public class Query {
    private final String sql;
    private final AccessConfiguration accessConfiguration;
    private final IDatabase database;
    private final List<Tuple> results;
    private final String resultPath;
}

