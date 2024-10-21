package engine.model.algebra.operators.sql.translator;

import java.util.List;

import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.SelectNotIn;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class TranslateSelectNotIn {

    public void translate(SelectNotIn operator, AlgebraTreeToSQLVisitor visitor) {
        if (visitor.getCurrentSelectNotIn() == null) {
            visitor.setCurrentSelectNotIn(operator);
            visitor.setSelectNotInBufferWhere(new StringBuilder());
        }
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase source = visitor.getSource();
        IDatabase target = visitor.getTarget();
        visitor.visitChildren(operator);
        result.append("\n").append(visitor.indentString());
        result.append(" LEFT JOIN ");
        IAlgebraOperator rightOperator = operator.getSelectionOperator();
        result.append("(");
        result.append("\n").append(visitor.indentString());
        rightOperator.accept(visitor);
        visitor.incrementIndentLevel();
        result.append("\n").append(visitor.indentString()).append(") AS ");
        result.append("Nest_").append(operator.hashCode());
        visitor.reduceIndentLevel();
        result.append("\n").append(visitor.indentString());
        result.append(" ON ");
        List<AttributeRef> leftAttributes = operator.getAttributes(source, target);
        List<AttributeRef> rightAttributes = rightOperator.getAttributes(source, target);
        assert (!leftAttributes.isEmpty());
        for (int i = 0; i < leftAttributes.size(); i++) {
            AttributeRef leftAttribute = leftAttributes.get(i);
            AttributeRef rightAttribute = rightAttributes.get(i);
            result.append(DBMSUtility.attributeRefToSQLDot(leftAttribute));
            result.append(" = ");
            result.append(DBMSUtility.attributeRefToAliasSQL(rightAttribute));
            result.append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), result.getStringBuilder());
        result.append("\n").append(visitor.indentString());
        if (visitor.getSelectNotInBufferWhere().length() > 0) {
            visitor.getSelectNotInBufferWhere().append(" AND ");
        }
        for (int i = 0; i < rightAttributes.size(); i++) {
            AttributeRef rightAttribute = rightAttributes.get(i);
            visitor.getSelectNotInBufferWhere().append(DBMSUtility.attributeRefToAliasSQL(rightAttribute));
            visitor.getSelectNotInBufferWhere().append(" IS NULL ");
            visitor.getSelectNotInBufferWhere().append(" AND ");
        }
        EngineUtility.removeChars(" AND ".length(), visitor.getSelectNotInBufferWhere());
        if (visitor.getCurrentSelectNotIn() == operator) {
            result.append(" WHERE ");
            result.append("\n").append(visitor.indentString());
            result.append("\n").append(visitor.getSelectNotInBufferWhere());
            visitor.setCurrentSelectNotIn(null);
        }
    }
}
