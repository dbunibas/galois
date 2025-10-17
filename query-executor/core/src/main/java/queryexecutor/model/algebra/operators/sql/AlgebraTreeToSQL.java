package queryexecutor.model.algebra.operators.sql;

import queryexecutor.model.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.operators.sql.translator.AlgebraTreeToSQLVisitor;

public class AlgebraTreeToSQL {

    private static Logger logger = LoggerFactory.getLogger(AlgebraTreeToSQL.class);

    public String treeToSQL(IAlgebraOperator root, IDatabase source, IDatabase target, String initialIndent) {
        if (logger.isDebugEnabled()) logger.debug("Generating SQL for algebra \n" + root);
        AlgebraTreeToSQLVisitor visitor = new AlgebraTreeToSQLVisitor(source, target, initialIndent);
        root.accept(visitor);
        if (logger.isDebugEnabled()) logger.debug("Resulting query: \n" + visitor.getResult());
        return visitor.getResult();
    }

}
