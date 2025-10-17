package queryexecutor.model.database.operators.dbms;

import queryexecutor.exceptions.DBMSException;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.operators.sql.AlgebraTreeToSQL;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.DBMSVirtualDB;
import queryexecutor.model.database.operators.IExplainQuery;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.persistence.xml.DAOXmlUtility;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdom.Document;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExplainQuery implements IExplainQuery {

    private static Logger logger = LoggerFactory.getLogger(SQLExplainQuery.class);

    private AlgebraTreeToSQL translator = new AlgebraTreeToSQL();
    private DAOXmlUtility daoUtility = new DAOXmlUtility();

    public long explain(IAlgebraOperator query, IDatabase source, IDatabase target) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXPLAIN (");
        sb.append("format xml) ");
        sb.append(translator.treeToSQL(query, source, target, "\t"));
        ResultSet rs = null;
        try {
            rs = QueryManager.executeQuery(sb.toString(), getAccessConfiguration(target));
            rs.next();
            String explainString = rs.getString(1);
            if (logger.isDebugEnabled()) logger.debug(explainString);
            Document doc = daoUtility.buildDOMFromString(explainString);
            Namespace ns = Namespace.getNamespace("http://www.postgresql.org/2009/explain");
            String nodeName = "Plan-Rows";
            String actualeRowsString = doc.getRootElement().getChild("Query", ns).getChild("Plan", ns).getChild(nodeName, ns).getText();
            return Long.parseLong(actualeRowsString);
        } catch (SQLException ex) {
            throw new DBMSException("Unable to execute query " + sb + " on database \n" + getAccessConfiguration(target) + "\n" + ex);
        } finally {
            QueryManager.closeResultSet(rs);
        }
    }

    private AccessConfiguration getAccessConfiguration(IDatabase target) throws IllegalArgumentException {
        AccessConfiguration accessConfiguration;
        if (target instanceof DBMSDB) {
            accessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        } else if (target instanceof DBMSVirtualDB) {
            accessConfiguration = ((DBMSVirtualDB) target).getAccessConfiguration();
        } else {
            throw new IllegalArgumentException("Unable to execute SQL on main memory db.");
        }
        return accessConfiguration;
    }
}
