package queryexecutor.model.database.operators.dbms;

import queryexecutor.exceptions.DBMSException;
import queryexecutor.model.database.operators.IRunQuery;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.operators.ITupleIterator;
import queryexecutor.model.algebra.operators.sql.AlgebraTreeToSQL;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ResultInfo;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.DBMSTupleIterator;
import queryexecutor.model.database.dbms.DBMSVirtualDB;
import queryexecutor.persistence.relational.AccessConfiguration;
import queryexecutor.persistence.relational.QueryManager;
import queryexecutor.utility.DBMSUtility;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.Tuple;
import queryexecutor.utility.QueryExecutorUtility;

public class SQLRunQuery implements IRunQuery {

    private static Logger logger = LoggerFactory.getLogger(SQLRunQuery.class);

    private AlgebraTreeToSQL translator = new AlgebraTreeToSQL();

    public ITupleIterator run(IAlgebraOperator query, IDatabase source, IDatabase target) {
        if (QueryExecutorConstants.DBMS_DEBUG) {
            return runDebug(query, source, target);
        } else {
            return runStandard(query, source, target);
        }
    }

    private ITupleIterator runStandard(IAlgebraOperator operator, IDatabase source, IDatabase target) {
        AccessConfiguration accessConfiguration = DBMSUtility.getAccessConfiguration(target);
        if (logger.isDebugEnabled()) logger.debug("Executing query \n" + operator);
        String sqlCode = translator.treeToSQL(operator, source, target, "");
        if (logger.isDebugEnabled()) logger.debug("Executing sql \n" + sqlCode);
        ResultSet resultSet = QueryManager.executeQuery(sqlCode, accessConfiguration);
        return new DBMSTupleIterator(resultSet);
    }

    public ResultInfo getSize(IAlgebraOperator operator, IDatabase source, IDatabase target) {
        AccessConfiguration accessConfiguration = DBMSUtility.getAccessConfiguration(target);
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("count(*) as count");
        AttributeRef oidAttribute = QueryExecutorUtility.getFirstOIDAttribute(operator.getAttributes(source, target));
        if (oidAttribute != null) {
            String oidAttributeSQL = DBMSUtility.attributeRefToAliasSQL(oidAttribute);
            query.append(", min(").append(oidAttributeSQL).append(") as min_oid");
            query.append(", max(").append(oidAttributeSQL).append(") as max_oid");
        }
        query.append(" FROM (\n");
        query.append(translator.treeToSQL(operator, source, target, "\t"));
        query.append(") as v");
        if (logger.isDebugEnabled()) logger.debug("GetSize query\n\t" + query);
        ResultSet resultSet = null;
        try {
            resultSet = QueryManager.executeQuery(query.toString(), accessConfiguration);
            resultSet.next();
            long count = resultSet.getLong("count");
            ResultInfo result = new ResultInfo(count);
            if (oidAttribute != null) {
                result.setMinOid(resultSet.getLong("min_oid"));
                result.setMaxOid(resultSet.getLong("max_oid"));
            }
            return result;
        } catch (SQLException ex) {
            throw new DBMSException("Unable to execute query " + query + " on database \n" + accessConfiguration + "\n" + ex);
        } finally {
            QueryManager.closeResultSet(resultSet);
        }
    }

    ///////////////////////////
    /////////     DEBUG
    ///////////////////////////
    private ITupleIterator runDebug(IAlgebraOperator query, IDatabase source, IDatabase target) {
        //DEBUG MODE
        AccessConfiguration accessConfiguration;
        if (target instanceof DBMSDB) {
            accessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        } else if (target instanceof DBMSVirtualDB) {
            accessConfiguration = ((DBMSVirtualDB) target).getAccessConfiguration();
        } else {
            throw new IllegalArgumentException("Unable to execute SQL on main memory db.");
        }
        if (logger.isDebugEnabled()) logger.debug("Executing query \n" + query);
        ITupleIterator mainMemoryTupleIterator = query.execute(source, target);
        String sqlCode = translator.treeToSQL(query, source, target, "");
        ResultSet resultSet = QueryManager.executeQuery(sqlCode, accessConfiguration);
        ITupleIterator dbmsTupleIterator = new DBMSTupleIterator(resultSet);
        compare(mainMemoryTupleIterator, dbmsTupleIterator, query, sqlCode);
        mainMemoryTupleIterator.close();
        dbmsTupleIterator.reset();
        return dbmsTupleIterator;
    }

    private void compare(ITupleIterator mainMemoryTupleIterator, ITupleIterator dbmsTupleIterator, IAlgebraOperator query, String sqlCode) {
        List<String> mainMemoryResult = materializeResult(mainMemoryTupleIterator);
        List<String> dbmsResult = materializeResult(dbmsTupleIterator);
        Collections.sort(mainMemoryResult);
        Collections.sort(dbmsResult);
        if (mainMemoryResult.size() != dbmsResult.size() || !mainMemoryResult.equals(dbmsResult)) {
            logger.error("\n\n\n#############################\n");
            logger.error("\n#### Query:\n" + query);
            logger.error("\n#### SQLCode:\n" + sqlCode);
            logger.error("\n#### MainMemoryResult:\n" + QueryExecutorUtility.printCollection(mainMemoryResult));
            logger.error("\n#### DbmsResult:\n" + QueryExecutorUtility.printCollection(dbmsResult));
            logger.error("\n#############################\n\n\n");
//            throw new IllegalArgumentException("WRONG EXECUTION");
        }
    }

    private List<String> materializeResult(ITupleIterator iterator) {
        List<String> result = new ArrayList<String>();
        while (iterator.hasNext()) {
            result.add(createString(iterator.next()));
        }
        return result;
    }

    private String createString(Tuple next) {
        String result = "";
        for (Cell cell : next.getCells()) {
            if (!cell.isOID()) {
                result += cell.getAttribute() + ":" + cell.getValue() + " | ";
            }
        }
        return result.toLowerCase();
    }

    public boolean isUseTrigger() {
        return true;
    }
}
