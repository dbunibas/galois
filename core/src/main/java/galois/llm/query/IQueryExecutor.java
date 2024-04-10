package galois.llm.query;

import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.List;

public interface IQueryExecutor {
    // TODO: In order to be more generic, switch ITable to IDatabase
    List<Tuple> execute(ITable table, TableAlias tableAlias);
}
