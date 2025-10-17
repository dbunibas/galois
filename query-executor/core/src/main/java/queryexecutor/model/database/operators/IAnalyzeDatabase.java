package queryexecutor.model.database.operators;

import queryexecutor.model.database.IDatabase;


public interface IAnalyzeDatabase {
    
    public void analyze(IDatabase database, int maxNumberOfThreads);

}
