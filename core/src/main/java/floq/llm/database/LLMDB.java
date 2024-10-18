package floq.llm.database;

import engine.model.database.ITable;
import engine.model.database.dbms.DBMSDB;
import engine.persistence.relational.AccessConfiguration;

public class LLMDB extends DBMSDB {
    public LLMDB(AccessConfiguration accessConfiguration) {
        super(accessConfiguration);
    }

    @Override
    public ITable getTable(String name) {
        super.getTable(name);
        return new LLMTable(name, getAccessConfiguration());
    }
}
