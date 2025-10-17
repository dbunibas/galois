package queryexecutor.model.algebra.aggregatefunctions;

import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.Tuple;
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
