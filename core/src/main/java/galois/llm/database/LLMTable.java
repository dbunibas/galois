package galois.llm.database;

import speedy.model.database.dbms.DBMSTable;
import speedy.persistence.relational.AccessConfiguration;

public class LLMTable extends DBMSTable {
    public LLMTable(String name, AccessConfiguration accessConfiguration) {
        super(name, accessConfiguration);
    }
}
