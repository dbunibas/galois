package galois.test.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import speedy.model.database.IDatabase;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.file.CSVFile;
import speedy.utility.DBMSUtility;

import java.io.File;
import java.util.Map;

import static galois.test.evaluation.CSVFileHandler.createDataFilesInExperimentFolder;

@Slf4j
public class DatabaseInitializer {
    public static void initializeDatabaseFromExperimentFolder(String experimentFolder, IDatabase database, SchemaDatabase schema) {
        if (!(database instanceof DBMSDB db) || DBMSUtility.isDBExists(db.getAccessConfiguration())) {
            log.info("Database has already been initialized!");
            return;
        }

        // FIXME: when creating the tables from files primary keys and other constraints should be added!
        db.getInitDBConfiguration().setCreateTablesFromFiles(true);
        Map<String, CSVFile> filesToImport = createDataFilesInExperimentFolder(experimentFolder, schema);
        filesToImport.forEach((name, file) -> db.getInitDBConfiguration().addFileToImportForTable(name, file));
        db.initDBMS();

        filesToImport.values().forEach(csv -> FileUtils.deleteQuietly(new File(csv.getFileName())));
    }
}
