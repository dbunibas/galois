package queryexecutor.model.algebra.operators;

import java.util.List;
import java.util.Set;

import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;

public interface ICreateTable {

    public void createTable(String tableName, List<Attribute> attributes, IDatabase target);

    public void createTable(String tableName, List<Attribute> attributes, Set<String> primaryKeys, IDatabase target);

}
