package engine.model.algebra.aggregatefunctions;

import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.Tuple;
import java.util.List;

public interface IAggregateFunction extends Cloneable{

    IValue evaluate(IDatabase db, List<Tuple> tuples);

    String getName();

    AttributeRef getAttributeRef();

    void setAttributeRef(AttributeRef attributeRef);

    AttributeRef getNewAttributeRef();

    void setNewAttributeRef(AttributeRef newAttributeRef);
    
    IAggregateFunction clone();

}
