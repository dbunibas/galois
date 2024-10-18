package floq.llm.database;

import engine.model.database.dbms.DBMSTable;
import engine.persistence.relational.AccessConfiguration;

public class LLMTable extends DBMSTable {
    public LLMTable(String name, AccessConfiguration accessConfiguration) {
        super(name, accessConfiguration);
    }
}
