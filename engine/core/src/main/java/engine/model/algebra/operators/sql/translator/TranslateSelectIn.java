package engine.model.algebra.operators.sql.translator;

import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Join;
import engine.model.algebra.Select;
import engine.model.algebra.SelectIn;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

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
            EngineUtility.removeChars(", ".length(), result.getStringBuilder());
            result.append(") IN (");
            result.append("\n").append(visitor.indentString());
            visitor.incrementIndentLevel();
            selectionOperator.accept(visitor);
            visitor.reduceIndentLevel();
            result.append("\n").append(visitor.indentString());
            result.append(")");
            result.append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), result.getStringBuilder());
    }
}
