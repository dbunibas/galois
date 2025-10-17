package bsf.llm.database;

import queryexecutor.model.database.dbms.DBMSTable;
import queryexecutor.persistence.relational.AccessConfiguration;

public class LLMTable extends DBMSTable {
    public LLMTable(String name, AccessConfiguration accessConfiguration) {
        super(name, accessConfiguration);
    }
}
