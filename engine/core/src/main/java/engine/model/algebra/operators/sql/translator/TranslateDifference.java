package engine.model.algebra.operators.sql.translator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.Difference;
import engine.model.algebra.IAlgebraOperator;

public class TranslateDifference {

    private final static Logger logger = LoggerFactory.getLogger(TranslateDifference.class);

    public void translate(Difference operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        if (logger.isDebugEnabled()) logger.debug("Visiting difference");
        IAlgebraOperator leftChild = operator.getChildren().get(0);
        if (logger.isDebugEnabled()) logger.debug("# Left child for difference: \n" + leftChild);
        result.append("(\n");
        leftChild.accept(visitor);
        result.append("\n").append(visitor.indentString());
        result.append(") EXCEPT (\n");
        IAlgebraOperator rightChild = operator.getChildren().get(1);
        if (logger.isDebugEnabled()) logger.debug("# Right child for difference: \n" + rightChild);
        visitor.incrementIndentLevel();
        rightChild.accept(visitor);
        visitor.reduceIndentLevel();
        result.append("\n)");
    }

}
