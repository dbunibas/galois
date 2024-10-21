package engine.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.exceptions.DAOException;
import engine.model.database.mainmemory.MainMemoryDB;
import engine.model.database.mainmemory.datasource.DataSource;
import engine.model.database.operators.mainmemory.ImportCSVFileMainMemory;
import engine.persistence.xml.DAOXsd;

public class DAOMainMemoryDatabase {

    private DAOXsd daoXSD = new DAOXsd();
    private ImportCSVFileMainMemory daoFile = new ImportCSVFileMainMemory();
    private static Logger logger = LoggerFactory.getLogger(DAOMainMemoryDatabase.class);

    public MainMemoryDB loadXMLDatabase(String schemaFile, String instanceFile) throws DAOException {
        logger.debug("Loading main-memory database. Schema " + schemaFile + ". Instance " + instanceFile);
        DataSource dataSource = daoXSD.loadSchema(schemaFile);
        if (instanceFile != null) {
            if (logger.isDebugEnabled()) logger.debug("Loading instance");
            daoXSD.loadInstance(dataSource, instanceFile);
        } else {
            PersistenceUtility.createEmptyTables(dataSource);
        }
        return new MainMemoryDB(dataSource);
    }

    public MainMemoryDB loadCSVDatabase(String instancePath, char separator, Character quoteCharacter, boolean convertSkolemInHash) throws DAOException {
        return loadCSVDatabase(instancePath, separator, quoteCharacter, convertSkolemInHash, false);
    }
    
    public MainMemoryDB loadCSVDatabase(String instancePath, char separator, Character quoteCharacter, boolean convertSkolemInHash, boolean header) throws DAOException {
        logger.debug("Loading main-memory database. From CSV instances in folder: " + instancePath);
        DataSource dataSource = daoFile.loadSchema(instancePath, separator, quoteCharacter, header);
        daoFile.loadInstance(dataSource, instancePath, separator, quoteCharacter, convertSkolemInHash, header);
        return new MainMemoryDB(dataSource);
    }

}
