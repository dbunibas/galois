package engine.model.database;

import java.util.Iterator;
import engine.model.algebra.operators.ITupleIterator;
import java.util.List;
import engine.model.database.operators.lazyloading.ITupleLoader;

public interface ITable {

    public String getName();

    public List<Attribute> getAttributes();

    public ITupleIterator getTupleIterator();
    
    public Iterator<ITupleLoader> getTupleLoaderIterator();

    public String printSchema(String indent);

    public String toString(String indent);

    public String toStringWithSort(String indent);

    public String toShortString();

    public long getSize();
    
    public long getNumberOfDistinctTuples();

    public ITupleIterator getTupleIterator(int offset, int limit);

    public String getPaginationQuery(int offset, int limit);

    public Attribute getAttribute(String name);
}
