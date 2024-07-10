package galois.test.experiments;

import lombok.Data;
import speedy.model.database.IDatabase;
import speedy.persistence.relational.AccessConfiguration;


@Data
public class Query {
    private final String sql;
    private final AccessConfiguration accessConfiguration;
    private final IDatabase database;
//    private final List<Tuple> results;
    private final String resultPath;
}

