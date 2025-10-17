package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Offset;

public class TranslateOffset {

    public void translate(Offset operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
        result.append("\n").append(visitor.indentString());
        result.append("OFFSET ").append(operator.getOffset());
        result.append("\n");
    }

}
