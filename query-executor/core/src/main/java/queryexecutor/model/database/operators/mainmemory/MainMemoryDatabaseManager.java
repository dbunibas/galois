package queryexecutor.model.database.operators.mainmemory;

import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.mainmemory.MainMemoryDB;
import queryexecutor.model.database.mainmemory.datasource.DataSource;
import queryexecutor.model.database.mainmemory.datasource.INode;
import queryexecutor.model.database.mainmemory.datasource.IntegerOIDGenerator;
import queryexecutor.model.database.mainmemory.datasource.nodes.TupleNode;
import queryexecutor.model.database.operators.IDatabaseManager;
import queryexecutor.persistence.PersistenceConstants;

public class MainMemoryDatabaseManager implements IDatabaseManager {

    public IDatabase createDatabase(IDatabase target, String suffix) {
        INode schemaNode = new TupleNode(PersistenceConstants.DATASOURCE_ROOT_LABEL, IntegerOIDGenerator.getNextOID());
        schemaNode.setRoot(true);
//        generateSchema(schemaNode, (MainMemoryDB) database, affectedAttributes);
        DataSource deltaDataSource = new DataSource(PersistenceConstants.TYPE_META_INSTANCE, schemaNode);
        MainMemoryDB database = new MainMemoryDB(deltaDataSource);
//        generateInstance(database, (MainMemoryDB) database, rootName, affectedAttributes);
        return database;
    }

    public IDatabase cloneTarget(IDatabase target, String suffix) {
        return target.clone();
    }

    public void removeClone(IDatabase target, String suffix) {
    }

    public void removeTable(String tableName, IDatabase deltaDB) {
    }

    public void addUniqueConstraints(IDatabase db) {
    }

    public void initDatabase(IDatabase source, IDatabase target, boolean cleanTarget, boolean preventInsertDuplicateTuples) {
    }
}
