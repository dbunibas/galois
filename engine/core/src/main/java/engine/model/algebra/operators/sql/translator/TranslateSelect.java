package engine.model.algebra.operators.sql.translator;

import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Limit;
import engine.model.algebra.Offset;
import engine.model.algebra.OrderBy;
import engine.model.algebra.Select;

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
