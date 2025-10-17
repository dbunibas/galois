package queryexecutor.model.algebra.operators.sql.translator;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.Scan;
import queryexecutor.model.database.TableAlias;

public class TranslateScan {

    private final static Logger logger = LoggerFactory.getLogger(TranslateScan.class);

    public void translate(Scan operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        if (logger.isDebugEnabled()) logger.debug("Visiting scan " + operator);
        visitor.createSQLSelectClause(operator, new ArrayList<NestedOperator>(), true);
        result.append(" FROM ");
        TableAlias tableAlias = operator.getTableAlias();
        result.append(visitor.tableAliasToSQL(tableAlias));
    }

}
