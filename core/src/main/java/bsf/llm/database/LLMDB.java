package bsf.llm.database;

import queryexecutor.model.database.ITable;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.persistence.relational.AccessConfiguration;

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
