package galois.llm.query;

import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;

import java.util.List;

public interface IQueryExecutor {
    List<Tuple> execute(IDatabase table, TableAlias tableAlias);
}
