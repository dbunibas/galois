package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Join;
import queryexecutor.model.algebra.Select;
import queryexecutor.model.algebra.SelectIn;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.utility.DBMSUtility;
import queryexecutor.utility.QueryExecutorUtility;

public class TranslateSelectIn {

    public void translate(SelectIn operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase source = visitor.getSource();
        IDatabase target = visitor.getTarget();
        visitor.visitChildren(operator);
        result.append("\n").append(visitor.indentString());
        if (operator.getChildren() != null
                && (operator.getChildren().get(0) instanceof Select
                || operator.getChildren().get(0) instanceof Join)) {
            result.append(" AND ");
        } else {
            result.append(" WHERE ");
        }
//            result.append(" WHERE (");
        for (IAlgebraOperator selectionOperator : operator.getSelectionOperators()) {
            result.append("(");
            for (AttributeRef attributeRef : operator.getAttributes(source, target)) {
                result.append(DBMSUtility.attributeRefToSQLDot(attributeRef)).append(", ");
            }
            QueryExecutorUtility.removeChars(", ".length(), result.getStringBuilder());
            result.append(") IN (");
            result.append("\n").append(visitor.indentString());
            visitor.incrementIndentLevel();
            selectionOperator.accept(visitor);
            visitor.reduceIndentLevel();
            result.append("\n").append(visitor.indentString());
            result.append(")");
            result.append(" AND ");
        }
        QueryExecutorUtility.removeChars(" AND ".length(), result.getStringBuilder());
    }
}
