package engine.model.database.operators;

import engine.model.database.IDatabase;
import engine.model.database.mainmemory.datasource.OID;

public interface IOIDGenerator {

    OID getNextOID(String tableName);
    void addCounter(String tableName, int size);
    void initializeOIDs(IDatabase database);
}
