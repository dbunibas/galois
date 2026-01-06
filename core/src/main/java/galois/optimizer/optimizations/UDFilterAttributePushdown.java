package galois.optimizer.optimizations;

import galois.optimizer.IOptimization;
import galois.optimizer.OptimizerException;
import galois.udf.GaloisUDFilterAttribute;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.Select;
import speedy.model.algebra.Scan;
import speedy.model.algebra.udf.UserDefinedFunction;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;

@Slf4j
public class UDFilterAttributePushdown implements IOptimization {

    @Override
    public IAlgebraOperator optimize(IDatabase database, IAlgebraOperator query) {
        if (!(query instanceof UserDefinedFunction udf)) {
            throw new OptimizerException("UDFilterAttributePushdown: query is not a UserDefinedFunction: " + query);
        }

        IUserDefinedFunction udfFunc = udf.getFunction();
        if (!(udfFunc instanceof GaloisUDFilterAttribute gfa)) {
            log.debug("UDFilterAttributePushdown: UDF is not GaloisUDFilterAttribute ({}), skipping pushdown", udfFunc.getClass());
            return udf; // not the attribute-based UDF, nothing to do
        }

        if (udf.getChildren().isEmpty()) {
            throw new OptimizerException("UDFilterAttributePushdown: UDF has no children");
        }

        IAlgebraOperator child = udf.getChildren().get(0);
        if (!(child instanceof Scan scan)) {
            throw new OptimizerException("UDFilterAttributePushdown: child is not a Scan: " + child);
        }

        TableAlias tableAlias = scan.getTableAlias();
        String raw = gfa.getAttributeName();
        // attribute may be 'a.is_dead' or 'is_dead'
        String attributeName = raw.contains(".") ? raw.substring(raw.indexOf('.') + 1) : raw;

        // If we have a database, check the table for the attribute and skip pushdown if missing.
        if (database != null) {
            ITable table = database.getTable(tableAlias.getTableName());
            try {
                // getAttribute throws IllegalArgumentException when not found
                table.getAttribute(attributeName);
            } catch (IllegalArgumentException e) {
                log.debug("UDFilterAttributePushdown: attribute {} not present in table {}, skipping pushdown", attributeName, table.getName());
                return udf; // do not modify plan
            }
        }

        // Build expression attribute == "True" (string literal to avoid being treated as a variable)
        Expression expression = new Expression(attributeName + " == \"True\"");
        expression.setVariableDescription(attributeName, new AttributeRef(tableAlias, attributeName));

        log.debug("UDFilterAttributePushdown: pushdown expression {} for attribute {}", expression, attributeName);

        Select select = new Select(expression);
        select.addChild(scan);
        return select;
    }
}
