package engine.model.algebra.operators;

import engine.utility.AlgebraUtility;
import engine.EngineConstants;
import engine.exceptions.ExpressionSyntaxException;
import engine.model.database.AttributeRef;
import engine.model.database.Tuple;
import engine.model.expressions.Expression;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.IValue;
import engine.model.database.IVariableDescription;
import engine.model.database.NullValue;

public class EvaluateExpression {

    private static Logger logger = LoggerFactory.getLogger(EvaluateExpression.class);
        
    public Object evaluateFunction(Expression expression, Tuple tuple) throws ExpressionSyntaxException {
        if (logger.isDebugEnabled()) logger.debug("Evaluating function: " + expression + " on tuple " + tuple);
        setVariableValues(expression, tuple);
        Object value = expression.getJepExpression().getValueAsObject();
        if (expression.getJepExpression().hasError()) {
            throw new ExpressionSyntaxException(expression.getJepExpression().getErrorInfo());
        }
        if (logger.isDebugEnabled()) logger.debug("Value of function: " + value);
        return value;
    }

    public Object evaluateConditionRaw(Expression expression, Tuple tuple) throws ExpressionSyntaxException {
        if (logger.isDebugEnabled()) logger.debug("Evaluating condition: " + expression + " on tuple " + tuple);
        if (expression.toString().equalsIgnoreCase("true")) {
            return EngineConstants.TRUE;
        }
        if (tuple == null) return EngineConstants.FALSE;
        boolean contaiNulls = setVariableValues(expression, tuple);
        if (contaiNulls) {
            return EngineConstants.FALSE;
        }
        Object value = expression.getJepExpression().getValueAsObject();
        if (logger.isDebugEnabled()) logger.debug("Value of condition: {}", value);
        if (expression.getJepExpression().hasError()) {
            throw new ExpressionSyntaxException(expression.getJepExpression().getErrorInfo());
        }
        return value;
    }

    public Double evaluateCondition(Expression expression, Tuple tuple) throws ExpressionSyntaxException {
        Object value = evaluateConditionRaw(expression, tuple);
        if (expression.getJepExpression().hasError()) {
            throw new ExpressionSyntaxException(expression.getJepExpression().getErrorInfo());
        }
        try {
            Double result = Double.parseDouble(value.toString());
            return result;
        } catch (NumberFormatException numberFormatException) {
            logger.error(numberFormatException.toString());
            throw new ExpressionSyntaxException(numberFormatException.getMessage());
        }
    }

    private boolean setVariableValues(Expression expression, Tuple tuple) {
        if (logger.isDebugEnabled()) logger.debug("Evaluating expression " + expression.toLongString() + "\n on tuple " + tuple);
        JEP jepExpression = expression.getJepExpression();
        SymbolTable symbolTable = jepExpression.getSymbolTable();
        boolean containNulls = false;
        for (Variable jepVariable : symbolTable.getVariables()) {
            if (AlgebraUtility.isPlaceholder(jepVariable)) continue;
            Object variableDescription = jepVariable.getDescription();
            Object variableValue = findAttributeValue(tuple, variableDescription);
            assert (variableValue != null) : "Value of variable: " + jepVariable + " is null in tuple " + tuple;
            IValue cellValue = findValueForAttribute(tuple, variableDescription);
            if (logger.isTraceEnabled()) logger.trace("CellValue is null?:" + (cellValue instanceof NullValue));
            if (cellValue instanceof NullValue
                    && !expression.getExpressionString().toLowerCase().contains("not null")
                    && !expression.getExpressionString().toLowerCase().contains("is null")){
                containNulls = true;
//                continue;
                // TODO: is that true ? check it
            }
            if (logger.isTraceEnabled()) logger.trace("Setting var value: " + jepVariable.getDescription() + " = " + variableValue);
            jepExpression.setVarValue(jepVariable.getName(), variableValue);
        }
        return containNulls;
    }
    
    private Object findAttributeValue(Tuple tuple, Object description) {
        if (logger.isTraceEnabled()) logger.trace("Searching variable: " + description + " in tuple " + tuple);
        AttributeRef attributeRef = null;
        if (description instanceof IVariableDescription) {
            IVariableDescription variableDescription = (IVariableDescription) description;
            attributeRef = findOccurrenceInTuple(variableDescription, tuple);
        } else if (description instanceof AttributeRef) {
            attributeRef = (AttributeRef) description;
        } else {
            throw new IllegalArgumentException("Illegal variable description in expression: " + description + " of type " + description.getClass().getName());
        }
        return AlgebraUtility.getCellValue(tuple, attributeRef).toString();
    }
    
    private IValue findValueForAttribute(Tuple tuple, Object description) {
        if (logger.isTraceEnabled()) logger.trace("Searching variable: " + description + " in tuple " + tuple);
        AttributeRef attributeRef = findAttributeRef(tuple, description);
        return AlgebraUtility.getCellValue(tuple, attributeRef);
    }
    
    private AttributeRef findAttributeRef(Tuple tuple, Object description) {
        if (logger.isTraceEnabled()) logger.trace("Searching variable: " + description + " in tuple " + tuple);
        AttributeRef attributeRef = null;
        if (description instanceof IVariableDescription) {
            IVariableDescription variableDescription = (IVariableDescription) description;
            attributeRef = findOccurrenceInTuple(variableDescription, tuple);
        } else if (description instanceof AttributeRef) {
            attributeRef = (AttributeRef) description;
        } else {
            throw new IllegalArgumentException("Illegal variable description in expression: " + description + " of type " + description.getClass().getName());
        }
        return attributeRef;
    }

    private AttributeRef findOccurrenceInTuple(IVariableDescription variableDescription, Tuple tuple) {
        for (AttributeRef attributeRef : variableDescription.getAttributeRefs()) {
            if (AlgebraUtility.contains(tuple, attributeRef)) {
                return attributeRef;
            }
        }
        throw new IllegalArgumentException("Unable to find values for variable " + variableDescription.toString() + " in tuple " + tuple);
    }

}
