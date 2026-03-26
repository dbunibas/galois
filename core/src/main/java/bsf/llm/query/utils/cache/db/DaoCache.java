package bsf.llm.query.utils.cache.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import static bsf.Constants.*;

public class DaoCache implements AutoCloseable {
    private ConnectionSource connectionSource;
    private Dao<DBCacheEntry, String> dao;

    public void connect() throws SQLException {
        connectionSource = new JdbcConnectionSource(CACHE_DB_URI, CACHE_DB_USER, CACHE_DB_PASSWORD);
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

    @Override
    public void close() throws Exception {
        if (connectionSource != null) {
            connectionSource.close();
        }
    }
}
