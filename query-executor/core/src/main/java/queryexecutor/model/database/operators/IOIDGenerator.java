package queryexecutor.model.database.operators;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.mainmemory.datasource.OID;

public interface IOIDGenerator {

    OID getNextOID(String tableName);
    void addCounter(String tableName, int size);
    void initializeOIDs(IDatabase database);
}
