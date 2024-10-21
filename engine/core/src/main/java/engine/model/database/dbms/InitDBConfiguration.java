package engine.model.database.dbms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import engine.model.database.operators.dbms.IValueEncoder;
import engine.persistence.file.IImportFile;

public class InitDBConfiguration {

    private String initDBScript;
    private String postDBScript;
    private Map<String, List<IImportFile>> filesToImport = new HashMap<String, List<IImportFile>>();
    private boolean createTablesFromFiles = true;
    private boolean useCopyStatement = true;
    private IValueEncoder valueEncoder;
    private int numOfThreads = 1;

    public String getInitDBScript() {
        return initDBScript;
    }

    public void setInitDBScript(String initDBScript) {
        this.initDBScript = initDBScript;
    }

    public String getPostDBScript() {
        return postDBScript;
    }

    public void setPostDBScript(String postDBScript) {
        this.postDBScript = postDBScript;
    }

    public IValueEncoder getValueEncoder() {
        return valueEncoder;
    }

    public void setValueEncoder(IValueEncoder valueEncoder) {
        this.valueEncoder = valueEncoder;
    }

    public boolean isUseCopyStatement() {
        return useCopyStatement;
    }

    public void setUseCopyStatement(boolean useCopyStatement) {
        this.useCopyStatement = useCopyStatement;
    }

    public void addFileToImportForTable(String tableName, IImportFile fileToImport) {
        List<IImportFile> files = filesToImport.get(tableName);
        if (files == null) {
            files = new ArrayList<IImportFile>();
            this.filesToImport.put(tableName, files);
        }
        files.add(fileToImport);
    }

    public boolean hasFilesToImport() {
        return !filesToImport.isEmpty();
    }

    public List<IImportFile> getFilesToImport(String tableName) {
        return filesToImport.get(tableName);
    }

    public Set<String> getTablesToImport() {
        return filesToImport.keySet();
    }

    public boolean isCreateTablesFromFiles() {
        return createTablesFromFiles;
    }

    public void setCreateTablesFromFiles(boolean createTablesFromFiles) {
        this.createTablesFromFiles = createTablesFromFiles;
    }

    public boolean isEmpty() {
        return initDBScript == null && filesToImport.isEmpty();
    }

    public int getNumOfThreads() {
        return numOfThreads;
    }

    public void setNumOfThreads(int numOfThreads) {
        this.numOfThreads = numOfThreads;
    }

    @Override
    public String toString() {
        return "InitDBConfiguration{" + "initDBScript=" + initDBScript + ", filesToImport=" + filesToImport + ", createTablesFromXML=" + createTablesFromFiles + '}';
    }

}
