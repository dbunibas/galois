package engine.model.database.operators.mainmemory;

import engine.model.database.operators.IRunQuery;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.IDatabase;
import engine.model.database.ResultInfo;
import engine.model.database.Tuple;
import engine.model.database.TupleOID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMemoryRunQuery implements IRunQuery {

    private static Logger logger = LoggerFactory.getLogger(MainMemoryRunQuery.class);

    public ITupleIterator run(IAlgebraOperator query, IDatabase source, IDatabase target) {
        return query.execute(source, target);
    }

    public ResultInfo getSize(IAlgebraOperator query, IDatabase source, IDatabase target) {
        ITupleIterator iterator = this.run(query, source, target);
        long count = 0;
        long minOid = Long.MAX_VALUE;
        long maxOid = 0;
        while (iterator.hasNext()) {
            Tuple tuple = iterator.next();
            count++;
            TupleOID oidIValue = tuple.getOid();
            long oidLongValue = Long.parseLong(oidIValue.toString());
            if (oidLongValue > maxOid) {
                maxOid = oidLongValue;
            }
            if (oidLongValue < minOid) {
                minOid = oidLongValue;
            }
        }
        if (count == 0) {
            minOid = 0;
        }
        iterator.close();
        ResultInfo result = new ResultInfo(count);
        result.setMinOid(minOid);
        result.setMaxOid(maxOid);
        return result;
    }

    public boolean isUseTrigger() {
        return false;
    }
}