package queryexecutor.model.algebra.operators.sql.translator;

public class SQLQueryBuilder {

    private StringBuilder sb = new StringBuilder();
    private boolean distinct;

    public SQLQueryBuilder() {
    }

    public SQLQueryBuilder(Object initialString) {
        sb.append(initialString);
    }

    public StringBuilder append(Object s) {
        return sb.append(s);
    }

    public StringBuilder getStringBuilder() {
        return sb;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
