package engine.model.database.operators.mainmemory;

import engine.model.algebra.IAlgebraOperator;
import engine.model.database.operators.IExplainQuery;
import engine.model.database.operators.IRunQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.IDatabase;

public class MainMemoryExplainQuery implements IExplainQuery {

    private static Logger logger = LoggerFactory.getLogger(MainMemoryExplainQuery.class);
    private IRunQuery queryRunner = new MainMemoryRunQuery();

    public long explain(IAlgebraOperator query, IDatabase source, IDatabase target) {
        return queryRunner.getSize(query, source, target).getSize();
    }

}
