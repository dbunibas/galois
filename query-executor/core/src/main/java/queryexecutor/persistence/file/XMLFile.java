package queryexecutor.persistence.file;

import queryexecutor.QueryExecutorConstants;

public class XMLFile implements IImportFile {

    private String fileName;

    public XMLFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return QueryExecutorConstants.XML;
    }

}
