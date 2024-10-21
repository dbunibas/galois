package engine.model.database.operators;

import engine.model.database.IDatabase;


public interface IAnalyzeDatabase {
    
    public void analyze(IDatabase database, int maxNumberOfThreads);

}
