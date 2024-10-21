package engine.model.algebra.operators.sql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.IVariableDescription;
import engine.model.expressions.Expression;
import engine.utility.DBMSUtility;
import engine.utility.EngineUtility;

public class ExpressionToSQL {

    private final static Logger logger = LoggerFactory.getLogger(ExpressionToSQL.class);

    public String expressionToSQL(Expression expression, IDatabase source, IDatabase target) {
        return expressionToSQL(expression, true, source, target);
    }

    public String expressionToSQL(Expression expression, boolean useAlias, IDatabase source, IDatabase target) {
        if (logger.isDebugEnabled()) logger.debug("Converting expression " + expression + " - Use alias: " + useAlias);
        Expression expressionClone = expression.clone();
        String expressionString = expression.toString();
        List<Variable> jepVariables = expressionClone.getJepExpression().getSymbolTable().getVariables();
        List<String> variables = expressionClone.getVariables();
        if (expressionString.startsWith("isNull(") || expressionString.startsWith("isNotNull(")) {
            Variable var = jepVariables.get(0);
            String attributeName = extractAttributeNameFromVariable(var.getDescription().toString(), expressionClone, useAlias);
            var.setDescription(attributeName);
            String result = expressionClone.toSQLString();
            if (logger.isDebugEnabled()) logger.debug("Returning (a) " + result);
            return result;
        }
        if (expressionString.startsWith("\"") && expressionString.endsWith("\"") && expressionString.length() > 1) {
            String result = expressionString.substring(1, expressionString.length() - 1);
            if (logger.isDebugEnabled()) logger.debug("Returning (b) " + result);
            return result;
        }
        boolean castNeeded = checkIfCastIsNeeded(variables, expressionClone, source, target);
        for (String variable : variables) {
            String attributeName = extractAttributeNameFromVariable(variable, expressionClone, useAlias);
            Variable var = findVariableInExpression(expressionClone, variable);
            if (var != null) {
                if (castNeeded) {
                    attributeName = "CAST(" + attributeName + " as text)";
                }
                var.setDescription(attributeName);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Returning (c) " + expressionClone);
        return expressionClone.toSQLString();
    }

    private Variable findVariableInExpression(Expression expression, String varString) {
        SymbolTable symbolTable = expression.getJepExpression().getSymbolTable();
        Variable var = symbolTable.getVar(varString);
        if (var != null) {
            return var;
        }
        var = symbolTable.getVar("Source." + varString);
        if (var != null) {
            return var;
        }
        for (Variable variable : symbolTable.getVariables()) {
            if (varString.equals(variable.getDescription().toString())) {
                return variable;
            }
        }
        if (logger.isDebugEnabled()) logger.debug("No variable " + varString + " - Symbols: " + symbolTable);
        return null;
    }

    private boolean checkIfCastIsNeeded(List<String> variables, Expression expression, IDatabase source, IDatabase target) {
        Set<String> types = new HashSet<String>();
        for (String variable : variables) {
            AttributeRef attributeRef = getAttributeRefFromVariable(variable, expression);
            if (attributeRef == null) {
                continue;
            }
            Attribute attribute = EngineUtility.getAttribute(attributeRef, source, target);
            types.add(attribute.getType());
        }
        if (types.isEmpty()) {
            return false;
        }
        return types.size() > 1;
    }

    private AttributeRef getAttributeRefFromVariable(String variable, Expression expression) {
        Variable variableExpression = expression.getJepExpression().getVar(variable);
        for (Variable var : expression.getJepExpression().getSymbolTable().getVariables()) {
            if (var.getDescription() != null && var.getDescription().toString().equals(variable)) {
                variableExpression = var;
            }
        }
        Object objectVariable = variableExpression.getDescription();
        if (logger.isDebugEnabled()) logger.debug("Object variable class: " + objectVariable.getClass());
        if (objectVariable instanceof IVariableDescription) {
            IVariableDescription variableDescription = (IVariableDescription) variableExpression.getDescription();
            return variableDescription.getAttributeRefs().get(0);
        }
        if (objectVariable instanceof AttributeRef) {
            return (AttributeRef) objectVariable;
        }
        return null;
    }

    private String extractAttributeNameFromVariable(String variable, Expression expression, boolean useAlias) {
        if (logger.isDebugEnabled()) logger.debug("Extracting attribute name for variable " + variable + " in expression " + expression);
        Variable variableExpression = expression.getJepExpression().getVar(variable);
        for (Variable var : expression.getJepExpression().getSymbolTable().getVariables()) {
            if (var.getDescription() != null && var.getDescription().toString().equals(variable)) {
                variableExpression = var;
            }
        }
        if (variableExpression == null) {
            throw new IllegalArgumentException("Unknow variable " + variable + " in expression " + expression);
//            return variable;
        }
        Object objectVariable = variableExpression.getDescription();
        if (logger.isDebugEnabled()) logger.debug("Object variable class: " + objectVariable.getClass());
        if (objectVariable instanceof IVariableDescription) {
            IVariableDescription variableDescription = (IVariableDescription) variableExpression.getDescription();
            AttributeRef attributeRef = variableDescription.getAttributeRefs().get(0);
            String result;
            if (useAlias) {
                result = DBMSUtility.attributeRefToSQLDot(attributeRef);
            } else {
                result = DBMSUtility.attributeRefToSQL(attributeRef);
            }
            if (logger.isDebugEnabled()) logger.debug("Return IVariableDescription: " + result);
            return result;
        }
        if (objectVariable instanceof AttributeRef) {
            AttributeRef attributeRef = (AttributeRef) objectVariable;
            String result;
            if (useAlias) {
                result = DBMSUtility.attributeRefToSQLDot(attributeRef);
            } else {
                result = DBMSUtility.attributeRefToSQL(attributeRef);
            }
            if (logger.isDebugEnabled()) logger.debug("Return AttributeRef: " + result);
            return result;
        }
        if (logger.isDebugEnabled()) logger.debug("Return: " + variable);
        return variable;
    }
}
