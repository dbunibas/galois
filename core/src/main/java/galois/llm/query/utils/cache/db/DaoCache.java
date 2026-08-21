package galois.llm.query.utils.cache.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import galois.utils.Configuration;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public class DaoCache implements AutoCloseable {
    private ConnectionSource connectionSource;
    private Dao<DBCacheEntry, String> dao;

    public void connect() throws SQLException {
        connectionSource = new JdbcConnectionSource(
                Configuration.getInstance().getCacheDbUri(),
                Configuration.getInstance().getCacheDbUser(),
                Configuration.getInstance().getCacheDbPassword()
        );
        dao = DaoManager.createDao(connectionSource, DBCacheEntry.class);
        TableUtils.createTableIfNotExists(connectionSource, DBCacheEntry.class);
    }

    public boolean containsEntry(String cacheKey) throws SQLException {
        return dao.idExists(cacheKey);
    }

    public DBCacheEntry getEntry(String cacheKey) throws SQLException {
        return dao.queryForId(cacheKey);
    }

    public void updateEntry(DBCacheEntry cacheEntry) throws SQLException {
        dao.createOrUpdate(cacheEntry);
    }

    // Updates the entries in a single transaction, way faster than a statement each when loading many entries
    public void updateEntries(List<DBCacheEntry> cacheEntries) throws Exception {
        dao.callBatchTasks(() -> {
            for (DBCacheEntry cacheEntry : cacheEntries) {
                dao.createOrUpdate(cacheEntry);
            }
            return null;
        });
    }

    // Entries whose key is shorter than a SHA-256 hash were hashed with a legacy algorithm and must be rehashed
    public long countEntriesToRehash() throws SQLException {
        return dao.queryRawValue(String.format("SELECT COUNT(*) FROM entry WHERE LENGTH(cache_key) < %d", DBCacheEntry.CACHE_KEY_LENGTH));
    }

    public List<DBCacheEntry> getEntriesToRehash(int limit) throws SQLException {
        return dao.queryBuilder()
                .limit((long) limit)
                .where()
                .raw(String.format("LENGTH(cache_key) < %d", DBCacheEntry.CACHE_KEY_LENGTH))
                .query();
    }

    public int updateEntryKey(DBCacheEntry cacheEntry, String cacheKey) throws SQLException {
        return dao.updateId(cacheEntry, cacheKey);
    }

    public int deleteEntry(String cacheKey) throws SQLException {
        return dao.deleteById(cacheKey);
    }

    public <T> T callBatchTasks(Callable<T> batch) throws Exception {
        return dao.callBatchTasks(batch);
    }

    @Override
    public void close() throws Exception {
        if (connectionSource != null) {
            connectionSource.close();
        }
    }
}
