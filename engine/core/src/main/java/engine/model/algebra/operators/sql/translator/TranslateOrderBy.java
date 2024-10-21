package engine.model.algebra.operators.sql.translator;

import java.util.List;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.OrderBy;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class TranslateOrderBy {

    public void translate(OrderBy operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase source = visitor.getSource();
        IDatabase target = visitor.getTarget();
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
        result.append("\n").append(visitor.indentString());
        result.append("ORDER BY ");
        for (AttributeRef attributeRef : operator.getAttributes(source, target)) {
            AttributeRef matchingAttribute = findFirstMatchingAttribute(attributeRef, visitor.getCurrentProjectionAttribute());
            result.append(DBMSUtility.attributeRefToSQL(matchingAttribute)).append(", ");
        }
        EngineUtility.removeChars(", ".length(), result.getStringBuilder());
        if (OrderBy.ORDER_DESC.equals(operator.getOrder())) {
            result.append(" ").append(OrderBy.ORDER_DESC);
        }
        result.append("\n");
    }

    private AttributeRef findFirstMatchingAttribute(AttributeRef originalAttribute, List<AttributeRef> attributes) {
        for (AttributeRef attribute : attributes) {
            if (attribute.getTableName().equalsIgnoreCase(originalAttribute.getTableName()) && attribute.getName().equalsIgnoreCase(originalAttribute.getName())) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Unable to find attribute " + originalAttribute + " into " + attributes);
    }

}
