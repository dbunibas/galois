package bsf.test.experiments;

import lombok.AllArgsConstructor;
import lombok.Data;
import queryexecutor.model.database.IDatabase;
import queryexecutor.persistence.relational.AccessConfiguration;

@Data
@AllArgsConstructor
public class Query {
    private String sql;
    private final AccessConfiguration accessConfiguration;
    private final IDatabase database;
//    private final List<Tuple> results;
    private final String resultPath;
}

