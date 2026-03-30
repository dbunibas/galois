package bsf.llm.query;

public class Execution {

    private static Execution instance;
    private String currentDB;
    private String currentDBId;

    private Execution() {

    }

    public static Execution getInstance() {
        if (instance == null) {
            instance = new Execution();
        }
        return instance;
    }

    public String getCurrentDB() {
        return currentDB;
    }

    public void setCurrentDB(String currentDB) {
        this.currentDB = currentDB;
    }

    public String getCurrentDBId() {
        return currentDBId;
    }

    public void setCurrentDBId(String currentDBId) {
        this.currentDBId = currentDBId;
    }

}
