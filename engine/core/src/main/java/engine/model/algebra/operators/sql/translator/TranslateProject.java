package engine.model.algebra.operators.sql.translator;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.algebra.CreateTableAs;
import engine.model.algebra.Difference;
import engine.model.algebra.Distinct;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Join;
import engine.model.algebra.Limit;
import engine.model.algebra.OrderBy;
import engine.model.algebra.Project;
import engine.model.algebra.RestoreOIDs;
import engine.model.algebra.Scan;
import engine.model.algebra.Select;
import engine.model.algebra.SelectNotIn;
import engine.model.database.AttributeRef;

public class TranslateProject {

    private final static Logger logger = LoggerFactory.getLogger(TranslateProject.class);

    public void translate(Project operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        if (logger.isDebugEnabled()) logger.debug("Visiting project");
        IAlgebraOperator child = operator.getChildren().get(0);
        if (logger.isDebugEnabled()) logger.debug("Child: " + child.getClass().getName());
        if (child instanceof Project) {
            //Ignore Project of Project
            child = child.getChildren().get(0);
        }
        if (child instanceof Limit || child instanceof Distinct || child instanceof SelectNotIn) {
            List<NestedOperator> innerOperators = new ArrayList<NestedOperator>();
            for (AttributeRef nestedAttribute : visitor.getNestedAttributes(child)) {
                if (visitor.containsAlias(innerOperators, nestedAttribute.getTableAlias())) {
                    continue;
                }
                NestedOperator nestedOperator = new NestedOperator(child, nestedAttribute.getTableAlias());
                innerOperators.add(nestedOperator);
            }
            boolean useTableName = true;
            if (child instanceof SelectNotIn) {
                useTableName = false;
            }
            visitor.createSQLSelectClause(child, innerOperators, useTableName);
            result.append(" FROM ");
            visitor.generateNestedSelect(child);
            return;
        }
        if (!(child instanceof Scan) && !(child instanceof Join) && !(child instanceof Select)
                && !(child instanceof CreateTableAs) && !(child instanceof RestoreOIDs) && !(child instanceof Distinct)
                && !(child instanceof Difference) && !(child instanceof OrderBy) && !(child instanceof SelectNotIn)) {
            throw new IllegalArgumentException("Project of a " + child.getName() + " is not supported");
        }
        child.accept(visitor);
    }

}