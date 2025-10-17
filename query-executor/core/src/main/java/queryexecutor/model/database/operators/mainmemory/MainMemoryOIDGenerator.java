package queryexecutor.model.database.operators.mainmemory;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.mainmemory.datasource.IntegerOIDGenerator;
import queryexecutor.model.database.mainmemory.datasource.OID;
import queryexecutor.model.database.operators.IOIDGenerator;

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
