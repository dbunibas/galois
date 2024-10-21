package engine.model.database.operators;

import engine.model.database.IDatabase;

public interface IDatabaseManager {

    public IDatabase createDatabase(IDatabase target, String suffix);
    
    public IDatabase cloneTarget(IDatabase target, String suffix);

    public void removeClone(IDatabase target, String suffix);

    public void removeTable(String tableName, IDatabase deltaDB);
    
    public void addUniqueConstraints(IDatabase db);

    public void initDatabase(IDatabase source, IDatabase target, boolean cleanTarget, boolean preventInsertDuplicateTuples);

}
