package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Limit;
import queryexecutor.model.algebra.Offset;
import queryexecutor.model.algebra.OrderBy;
import queryexecutor.model.algebra.Select;

public class TranslateSelect {

    public void translate(Select operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IAlgebraOperator child = operator.getChildren().get(0);
        if (child instanceof OrderBy || child instanceof Offset || child instanceof Limit) {
            result.append("SELECT * FROM ");
            visitor.generateNestedSelect(child);
        } else {
            visitor.visitChildren(operator);
        }
        visitor.createWhereClause(operator, false);
    }

}
