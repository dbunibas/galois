package galois.udf;

import com.galois.sqlparser.IUserDefinedFunctionFactory;
import com.galois.sqlparser.ParseContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GaloisUDFFactory implements IUserDefinedFunctionFactory {
    @Override
    public IUserDefinedFunction getUserDefinedFunction(String name, ExpressionList<? extends Expression> expressions, ParseContext parseContext) {
        if (name.equals("udfilter")) return parseUDFilter(expressions, parseContext);
        throw new UnsupportedOperationException("Unimplemented UDF: " + name + "!");
    }

    private IUserDefinedFunction parseUDFilter(ExpressionList<? extends Expression> expressions, ParseContext parseContext) {
        // Parse user message
        Expression userMessageExpression = expressions.getFirst();
        checkExpressionType(userMessageExpression, StringValue.class, "UDFilter parse error: first argument is not a string!");
        String userMessage = ((StringValue) userMessageExpression).getValue();

        // Parse attribute refs
        List<AttributeRef> attributeRefs = new ArrayList<>();
        for (int i = 1; i < expressions.size(); i++) {
            Expression columnExpression = expressions.get(i);
            checkExpressionType(columnExpression, Column.class, "UDFilter parse error: " + i + 1 + "th argument is not a column!");
            TableAlias tableAlias = parseContext.getTableAliasFromColumn((Column) columnExpression);
            AttributeRef attributeRef = new AttributeRef(tableAlias, ((Column) columnExpression).getColumnName());
            attributeRefs.add(attributeRef);
        }

        return new GaloisUDFilter(userMessage, attributeRefs);
    }

    private void checkExpressionType(Expression expression, Class<? extends Expression> clazz, String error) {
        if (clazz.isInstance(expression)) return;
        throw new IllegalArgumentException(error);
    }
}
