package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.RestoreOIDs;

public class TranslateRestoreOIDs {

    public void translate(RestoreOIDs operator, AlgebraTreeToSQLVisitor visitor) {
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
    }

}
