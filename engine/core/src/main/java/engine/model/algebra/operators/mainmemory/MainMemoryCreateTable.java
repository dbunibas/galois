package engine.model.algebra.operators.mainmemory;

import java.util.List;
import java.util.Set;

import engine.model.algebra.operators.ICreateTable;
import engine.model.database.Attribute;
import engine.model.database.IDatabase;

public class MainMemoryCreateTable implements ICreateTable {

    public void createTable(String tableName, List<Attribute> attributes, IDatabase target) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO Implement method
    }

    @Override
    public void createTable(String tableName, List<Attribute> attributes, Set<String> primaryKeys, IDatabase target) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO Implement method
    }

}