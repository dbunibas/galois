package engine.model.database.operators.mainmemory;

import engine.model.database.IDatabase;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import engine.model.database.mainmemory.datasource.OID;
import engine.model.database.operators.IOIDGenerator;

public class MainMemoryOIDGenerator implements IOIDGenerator{

    public void initializeOIDs(IDatabase database) {
        //Nothing to do
    }

    public OID getNextOID(String tableName) {
        return IntegerOIDGenerator.getNextOID();
    }

    public void addCounter(String tableName, int size) {
        for (int i = 0; i < size; i++) {
            getNextOID(tableName);
        }
    }

}
