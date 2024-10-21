package engine.model.database.dbms;

import java.io.Serializable;

public class SQLQueryString implements Serializable {

    private transient static int counter = 0;
    private String id;
    private String query;

    public SQLQueryString(String query) {
        this(null, query);
    }

    public SQLQueryString(String id, String query) {
        if (id == null) {
            id = "q" + counter++;
        }
        this.id = id;
        this.query = query;
    }

    public String getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return id + ": " + query;
    }

}
