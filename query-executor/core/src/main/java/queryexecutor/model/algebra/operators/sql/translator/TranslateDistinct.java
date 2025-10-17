package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.Distinct;
import queryexecutor.model.algebra.IAlgebraOperator;

public class TranslateDistinct {

    public void translate(Distinct operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        result.setDistinct(true);
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
    }

}
