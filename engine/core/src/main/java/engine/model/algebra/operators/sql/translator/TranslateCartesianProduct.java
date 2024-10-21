package engine.model.algebra.operators.sql.translator;

import java.util.List;
import engine.model.algebra.CartesianProduct;
import engine.model.algebra.IAlgebraOperator;
import engine.model.database.AttributeRef;
import engine.utility.EngineUtility;

public class TranslateCartesianProduct {

    public void translate(CartesianProduct operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        result.append("SELECT * FROM ");
        for (IAlgebraOperator child : operator.getChildren()) {
            visitor.generateNestedSelect(child);
            result.append(", ");
        }
        List<AttributeRef> attributes = operator.getAttributes(visitor.getSource(), visitor.getTarget());
        visitor.setCurrentProjectionAttribute(attributes);
        EngineUtility.removeChars(", ".length(), result.getStringBuilder());
    }

}
