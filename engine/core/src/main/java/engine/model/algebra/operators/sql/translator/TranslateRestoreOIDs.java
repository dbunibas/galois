package engine.model.algebra.operators.sql.translator;

import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.RestoreOIDs;

public class TranslateRestoreOIDs {

    public void translate(RestoreOIDs operator, AlgebraTreeToSQLVisitor visitor) {
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
    }

}
