package engine.model.database.mainmemory;

import engine.model.algebra.operators.ITupleIterator;
import engine.EngineConstants;
import engine.utility.EngineUtility;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.ITable;
import engine.model.database.OidTupleComparator;
import engine.model.database.Tuple;
import engine.model.database.mainmemory.datasource.DataSource;
import engine.model.database.mainmemory.datasource.INode;
import engine.model.database.mainmemory.datasource.operators.CalculateSize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import engine.model.database.Cell;
import engine.model.database.operators.lazyloading.ITupleLoader;
import engine.model.database.operators.lazyloading.MainMemoryTupleLoaderIterator;

public class MainMemoryTable implements ITable {

    private DataSource dataSource;
    private MainMemoryDB database;

    public MainMemoryTable(DataSource dataSource, MainMemoryDB database) {
        this.dataSource = dataSource;
        this.database = database;
    }

    public String getName() {
        INode schema = dataSource.getSchema();
        return schema.getLabel();
    }

    public List<Attribute> getAttributes() {
        List<Attribute> result = new ArrayList<Attribute>();
        INode tupleNode = dataSource.getSchema().getChild(0);
        for (INode attributeNode : tupleNode.getChildren()) {
            result.add(new Attribute(getName(), attributeNode.getLabel(), attributeNode.getChild(0).getLabel()));
        }
        return result;
    }

    public Attribute getAttribute(String name) {
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Table " + getName() + " doesn't contain attribute " + name);
    }

    public long getSize() {
        CalculateSize calculator = new CalculateSize();
        return calculator.getNumberOfTuples(this.dataSource.getInstances().get(0));
    }

    public long getNumberOfDistinctTuples() {
        Set<String> distinct = new HashSet<String>();
        List<Attribute> attributes = getAttributes();
        ITupleIterator it = getTupleIterator();
        while (it.hasNext()) {
            Tuple tuple = it.next();
            StringBuilder sb = new StringBuilder();
            for (Attribute attribute : attributes) {
                Cell cell = tuple.getCell(new AttributeRef(getName(), attribute.getName()));
                sb.append(cell.toString()).append("|");
            }
            distinct.add(sb.toString());
        }
        it.close();
        return distinct.size();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public MainMemoryDB getDatabase() {
        return database;
    }

    public ITupleIterator getTupleIterator(int offset, int limit) {
        return getTupleIterator();
    }

    public String getPaginationQuery(int offset, int limit) {
        return "pagintion disabled";
    }

    public ITupleIterator getTupleIterator() {
        return new MainMemoryTupleIterator(this);
    }

    public Iterator<ITupleLoader> getTupleLoaderIterator() {
        return new MainMemoryTupleLoaderIterator(getTupleIterator());
    }

    public String printSchema(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append("{\n");
        for (Attribute attribute : getAttributes()) {
            result.append(indent).append(EngineConstants.INDENT);
            result.append(attribute.getName()).append(" ");
            result.append(attribute.getType()).append("\n");
        }
        result.append(indent).append("}\n");
        return result.toString();
    }

    public String toString() {
        return toString("");
    }

    public String toShortString() {
        return getName();
    }

    public String toString(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append(" {\n");
        ITupleIterator iterator = getTupleIterator();
        while (iterator.hasNext()) {
            result.append(indent).append(EngineConstants.INDENT).append(iterator.next().toStringWithOID()).append("\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    public String toStringWithSort(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("Table: ").append(getName()).append(" {\n");
        ITupleIterator iterator = getTupleIterator();
        List<Tuple> tuples = new ArrayList<Tuple>();
        while (iterator.hasNext()) {
            tuples.add(iterator.next());
        }
        Collections.sort(tuples, new OidTupleComparator());
        for (Tuple tuple : tuples) {
            result.append(indent).append(EngineConstants.INDENT);
//            for (Attribute attribute : getAttributes()) {
//                result.append(tuple.getCell(new AttributeRef(attribute.getTableName(), attribute.getName()))).append(", ");
//            }
            result.append(tuple.toStringWithOID());
            result.append("\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    public void closeConnection() {
    }

    class MainMemoryTupleIterator implements ITupleIterator {

        private MainMemoryTable table;
        private int pos = 0;
        private long size;

        public MainMemoryTupleIterator(MainMemoryTable table) {
            this.table = table;
            this.size = table.getSize();
        }

        public boolean hasNext() {
            return pos < size;
        }

        public Tuple next() {
            INode tupleNode = this.table.dataSource.getInstances().get(0).getChild(pos);
            Tuple tuple = EngineUtility.createTuple(tupleNode, table.getName());
            pos++;
            return tuple;
        }

        public void reset() {
            this.pos = 0;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() {
        }
    }
}
