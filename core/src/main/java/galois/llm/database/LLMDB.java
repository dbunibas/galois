package galois.llm.database;

import speedy.model.database.ITable;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTable;
import speedy.persistence.relational.AccessConfiguration;

public class LLMDB extends DBMSDB {
    public LLMDB(AccessConfiguration accessConfiguration) {
        super(accessConfiguration);
    }

    @Override
    public ITable getTable(String name) {
        if (name.contains("\"")) name = name.replace("\"", "");
        super.getTable(name);
        return new LLMTable(name, getAccessConfiguration());
    }
}
