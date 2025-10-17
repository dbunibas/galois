package queryexecutor.model.algebra.operators.sql.translator;

import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.OrderByRandom;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.persistence.relational.AccessConfiguration;

public class TranslateOrderByRandom {

    public void translate(OrderByRandom operator, AlgebraTreeToSQLVisitor visitor) {
        SQLQueryBuilder result = visitor.getSQLQueryBuilder();
        IDatabase target = visitor.getTarget();
        IAlgebraOperator child = operator.getChildren().get(0);
        child.accept(visitor);
        result.append("\n").append(visitor.indentString());
        result.append("ORDER BY ").append(getRandomFunction(((DBMSDB) target).getAccessConfiguration()));
        result.append("\n");
    }

    private String getRandomFunction(AccessConfiguration accessConfiguration) {
        if (accessConfiguration.getDriver().contains("postgres")) {
            return "RANDOM()";
        }
        if (accessConfiguration.getDriver().contains("mysql")) {
            return "RAND()";
        }
        throw new IllegalArgumentException("Unsupported DBMS " + accessConfiguration.getDriver());
    }

}
