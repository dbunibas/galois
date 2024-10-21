package engine.model.database.mainmemory;

import engine.EngineConstants;
import engine.utility.EngineUtility;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.operators.ITupleIterator;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.OidTupleComparator;
import engine.model.database.Tuple;
import engine.persistence.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import engine.model.database.Cell;
import engine.model.database.operators.lazyloading.ITupleLoader;
import engine.model.database.operators.lazyloading.MainMemoryTupleLoaderIterator;

public class MainMemoryVirtualTable implements ITable {

    private String name;
    private IAlgebraOperator query;
    private IDatabase database;
    private IDatabase originalDatabase;

    public MainMemoryVirtualTable(String name, IAlgebraOperator query, IDatabase database, IDatabase originalDatabase) {
        this.name = name;
        this.query = query;
        this.database = database;
        this.originalDatabase = originalDatabase;
    }

    public String getName() {
        return name;
    }

    public List<Attribute> getAttributes() {
        if (originalDatabase.getTableNames().contains(name)) {
            ITable originalTable = originalDatabase.getTable(name);
            return originalTable.getAttributes();
        }
        List<AttributeRef> attributeRefs = this.query.getAttributes(null, database);
        List<Attribute> result = new ArrayList<Attribute>();
        for (AttributeRef attributeRef : attributeRefs) {
            result.add(new Attribute(attributeRef.getTableName(), attributeRef.getName(), Types.STRING));
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
        long size = 0;
        ITupleIterator it = getTupleIterator();
        while (it.hasNext()) {
            it.next();
            size++;
        }
        it.close();
        return size;
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

    public IAlgebraOperator getQuery() {
        return query;
    }

    public void setQuery(IAlgebraOperator query) {
        this.query = query;
    }

    public ITupleIterator getTupleIterator(int offset, int limit) {
        return getTupleIterator();
    }

    public String getPaginationQuery(int offset, int limit) {
        return "pagintion disabled";
    }

    public ITupleIterator getTupleIterator() {
        return query.execute(null, database);
    }

    public Iterator<ITupleLoader> getTupleLoaderIterator() {
        return new MainMemoryTupleLoaderIterator(getTupleIterator());
    }

    public void addTuple(Tuple tuple) {
        throw new UnsupportedOperationException("Unable to add tuples to views");
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

    public String toShortString() {
        return getName();
    }

    public String toString() {
        return toString("");
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
            result.append("oid: ").append(tuple.getOid()).append(" ");
            result.append("[");
            for (Attribute attribute : getOriginalAttributes()) {
                result.append(attribute.getName()).append(":");
                result.append(tuple.getCell(new AttributeRef(attribute.getTableName(), attribute.getName())).getValue());
                result.append(", ");
            }
            EngineUtility.removeChars(", ".length(), result);
            result.append("]\n");
        }
        iterator.close();
        result.append(indent).append("}\n");
        return result.toString();
    }

    private List<Attribute> getOriginalAttributes() {
        return this.originalDatabase.getTable(name).getAttributes();
    }

    public void closeConnection() {
    }

}
