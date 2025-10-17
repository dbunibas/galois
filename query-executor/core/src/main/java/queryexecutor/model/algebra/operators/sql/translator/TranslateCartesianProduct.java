package queryexecutor.model.algebra.operators.sql.translator;

import java.util.List;
import queryexecutor.model.algebra.CartesianProduct;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.utility.QueryExecutorUtility;

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
        QueryExecutorUtility.removeChars(", ".length(), result.getStringBuilder());
    }

}
