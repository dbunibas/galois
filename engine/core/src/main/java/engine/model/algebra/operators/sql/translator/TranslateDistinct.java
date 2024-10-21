package engine.model.algebra.operators.sql.translator;

import engine.model.algebra.Distinct;
import engine.model.algebra.IAlgebraOperator;

public class TranslateDistinct {

    public void translate(Distinct operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        result.setDistinct(true);
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
    }

}
